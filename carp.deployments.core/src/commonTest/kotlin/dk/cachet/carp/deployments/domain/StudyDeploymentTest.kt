@file:Suppress( "LargeClass" )

package dk.cachet.carp.deployments.domain

import dk.cachet.carp.common.application.UUID
import dk.cachet.carp.common.application.data.CarpDataTypes
import dk.cachet.carp.common.application.data.DataType
import dk.cachet.carp.common.application.devices.AltBeaconDeviceRegistration
import dk.cachet.carp.common.application.devices.AnyDeviceDescriptor
import dk.cachet.carp.common.application.devices.AnyMasterDeviceDescriptor
import dk.cachet.carp.common.application.devices.DefaultDeviceRegistration
import dk.cachet.carp.common.application.tasks.Measure
import dk.cachet.carp.common.application.triggers.TaskControl
import dk.cachet.carp.common.application.users.UsernameAccountIdentity
import dk.cachet.carp.common.infrastructure.serialization.CustomDeviceDescriptor
import dk.cachet.carp.common.infrastructure.serialization.CustomMasterDeviceDescriptor
import dk.cachet.carp.common.infrastructure.serialization.createDefaultJSON
import dk.cachet.carp.common.infrastructure.test.STUB_DATA_TYPE
import dk.cachet.carp.common.infrastructure.test.StubDeviceDescriptor
import dk.cachet.carp.common.infrastructure.test.StubMasterDeviceDescriptor
import dk.cachet.carp.common.infrastructure.test.StubTaskDescriptor
import dk.cachet.carp.common.infrastructure.test.StubTrigger
import dk.cachet.carp.data.application.DataStreamsConfiguration
import dk.cachet.carp.deployments.application.DeviceDeploymentStatus
import dk.cachet.carp.deployments.application.MasterDeviceDeployment
import dk.cachet.carp.deployments.application.StudyDeploymentStatus
import dk.cachet.carp.deployments.application.users.ParticipantInvitation
import dk.cachet.carp.deployments.application.users.ParticipantStatus
import dk.cachet.carp.deployments.application.users.StudyInvitation
import dk.cachet.carp.protocols.domain.start
import dk.cachet.carp.protocols.infrastructure.test.createEmptyProtocol
import dk.cachet.carp.protocols.infrastructure.test.createSingleMasterDeviceProtocol
import dk.cachet.carp.protocols.infrastructure.test.createSingleMasterWithConnectedDeviceProtocol
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlin.test.*


/**
 * Tests for [StudyDeployment].
 */
@Suppress( "LargeClass" )
class StudyDeploymentTest
{
    companion object
    {
        private val JSON: Json = createDefaultJSON()
    }


    @Test
    fun fromInvitations_with_invalid_protocol_fails()
    {
        val protocol = createEmptyProtocol()
        val snapshot = protocol.getSnapshot()

        // Protocol does not contain a master device, thus contains deployment error and can't be initialized.
        assertFailsWith<IllegalArgumentException>
        {
            StudyDeployment.fromInvitations( snapshot, emptyList() )
        }
    }

    @Test
    fun fromInvitations_with_invalid_invitations_fails()
    {
        val protocol = createSingleMasterDeviceProtocol()
        val snapshot = protocol.getSnapshot()

        val incorrectInvitation = ParticipantInvitation(
            UUID.randomUUID(),
            setOf( "Invalid" ),
            UsernameAccountIdentity( "Test" ),
            StudyInvitation( "Test" )
        )
        assertFailsWith<IllegalArgumentException>
        {
            StudyDeployment.fromInvitations( snapshot, listOf( incorrectInvitation ) )
        }
    }

    @Test
    fun new_deployment_has_unregistered_master_device()
    {
        val protocol = createSingleMasterWithConnectedDeviceProtocol()
        val deployment: StudyDeployment = studyDeploymentFor( protocol )

        // Two devices can be registered, but none are by default.
        assertEquals( 2, deployment.registrableDevices.size )
        assertTrue { deployment.registrableDevices.map { it.device }.containsAll( protocol.devices ) }
        assertEquals( 0, deployment.registeredDevices.size )

        // Only the master device requires deployment.
        val requiredDeployment = deployment.registrableDevices.single { it.requiresDeployment }
        assertEquals( protocol.masterDevices.single(), requiredDeployment.device )
    }

    @Test
    fun requiredDataStreams_is_complete()
    {
        val masterDevice = StubMasterDeviceDescriptor()
        val connectedDevice = StubDeviceDescriptor()
        val protocol = createEmptyProtocol().apply {
            addMasterDevice( masterDevice )
            addConnectedDevice( connectedDevice, masterDevice )

            val trigger = addTrigger( masterDevice.atStartOfStudy() )
            val stubMeasure = Measure.DataStream( STUB_DATA_TYPE )
            val task = StubTaskDescriptor(
                "Task",
                listOf( stubMeasure, trigger.measure() ),
                "Description"

            )
            addTaskControl( trigger.start( task, masterDevice ) )
            val connectedDeviceTask = StubTaskDescriptor( "Connected task", listOf( stubMeasure ) )
            addTaskControl( trigger.start( connectedDeviceTask, connectedDevice ) )
        }
        val deployment: StudyDeployment = studyDeploymentFor( protocol )

        val dataStreams = deployment.requiredDataStreams
        assertEquals( deployment.id, dataStreams.studyDeploymentId )
        val expectedMasterDeviceTypes =
            listOf( STUB_DATA_TYPE, CarpDataTypes.TRIGGERED_TASK.type, CarpDataTypes.COMPLETED_TASK.type )
                .map { DataStreamsConfiguration.ExpectedDataStream( masterDevice.roleName, it ) }
        val expectedConnectedDeviceType =
            listOf( STUB_DATA_TYPE, CarpDataTypes.COMPLETED_TASK.type )
                .map { DataStreamsConfiguration.ExpectedDataStream( connectedDevice.roleName, it ) }
        assertEquals(
            ( expectedMasterDeviceTypes + expectedConnectedDeviceType ).toSet(),
            dataStreams.expectedDataStreams
        )
    }

    @Test
    fun registerDevice_succeeds()
    {
        val protocol = createEmptyProtocol()
        val device = StubMasterDeviceDescriptor()
        protocol.addMasterDevice( device )
        val deployment: StudyDeployment = studyDeploymentFor( protocol )

        val registration = DefaultDeviceRegistration( "0" )
        deployment.registerDevice( device, registration )

        assertEquals( 1, deployment.registeredDevices.size )
        val registered = deployment.registeredDevices.values.single()
        assertEquals( registration, registered )
        assertEquals( 1, deployment.deviceRegistrationHistory[ device ]?.count() )
        assertEquals( registration, deployment.deviceRegistrationHistory[ device ]?.last() )
        assertEquals( StudyDeployment.Event.DeviceRegistered( device, registration ), deployment.consumeEvents().last() )
    }

    @Test
    fun registerDevice_of_optional_master_device_triggers_redeployment()
    {
        // Deploy master device.
        val master = StubMasterDeviceDescriptor( "Master 1" )
        val optionalMaster = StubMasterDeviceDescriptor( "Master 2", true )
        val protocol = createEmptyProtocol().apply {
            addMasterDevice( master )
            addMasterDevice( optionalMaster )
        }
        val deployment = studyDeploymentFor( protocol )
        deployment.registerDevice( master, master.createRegistration() )
        val deviceDeployment = deployment.getDeviceDeploymentFor( master )
        deployment.deviceDeployed( master, deviceDeployment.lastUpdatedOn )
        assertTrue( deployment.getStatus() is StudyDeploymentStatus.Running )

        // Register dependent device.
        deployment.registerDevice( optionalMaster, optionalMaster.createRegistration() )
        val status = deployment.getStatus()
        assertTrue( status is StudyDeploymentStatus.DeployingDevices )
        val deviceStatus = status.getDeviceStatus( master )
        assertTrue( deviceStatus is DeviceDeploymentStatus.NeedsRedeployment )
    }

    @Test
    fun cant_registerDevice_not_part_of_deployment()
    {
        val protocol = createSingleMasterWithConnectedDeviceProtocol()
        val deployment: StudyDeployment = studyDeploymentFor( protocol )

        val invalidDevice = StubMasterDeviceDescriptor( "Not part of deployment" )
        val registration = DefaultDeviceRegistration( "0" )

        assertFailsWith<IllegalArgumentException>
        {
            deployment.registerDevice( invalidDevice, registration )
        }
        assertEquals( 0, deployment.consumeEvents().filterIsInstance<StudyDeployment.Event.DeviceRegistered>().count() )
    }

    @Test
    fun cant_registerDevice_if_already_registered()
    {
        val protocol = createEmptyProtocol()
        val device = StubMasterDeviceDescriptor()
        protocol.addMasterDevice( device )
        val deployment: StudyDeployment = studyDeploymentFor( protocol )

        deployment.registerDevice( device, DefaultDeviceRegistration( "0" ) )

        assertFailsWith<IllegalArgumentException>
        {
            deployment.registerDevice( device, DefaultDeviceRegistration( "1" ))
        }
        assertEquals( 1, deployment.consumeEvents().filterIsInstance<StudyDeployment.Event.DeviceRegistered>().count() )
    }

    /**
     * When the runtime type of devices is unknown, deployment cannot verify whether a registration is valid (this is implemented on the type definition).
     * However, rather than not supporting deployment, registration is simply considered valid and forwarded as is.
     */
    @Test
    fun can_registerDevice_for_unknown_types()
    {
        val protocol = createEmptyProtocol()
        val master = StubMasterDeviceDescriptor( "Unknown master" )
        val connected = StubDeviceDescriptor( "Unknown connected" )

        // Mimic that the types are unknown at runtime. When this occurs, they are wrapped in 'Custom...'.
        val masterCustom = CustomMasterDeviceDescriptor( "Irrelevant", JSON.encodeToString( StubMasterDeviceDescriptor.serializer(), master ), JSON )
        val connectedCustom = CustomDeviceDescriptor( "Irrelevant", JSON.encodeToString( StubDeviceDescriptor.serializer(), connected ), JSON )

        protocol.addMasterDevice( masterCustom )
        protocol.addConnectedDevice( connectedCustom, masterCustom )

        val deployment: StudyDeployment = studyDeploymentFor( protocol )
        deployment.registerDevice( masterCustom, DefaultDeviceRegistration( "0" ) )
        deployment.registerDevice( connectedCustom, DefaultDeviceRegistration( "1" ) )
    }

    @Test
    fun cant_registerDevice_already_in_use_by_different_role()
    {
        val protocol = createEmptyProtocol()
        val master = StubMasterDeviceDescriptor( "Master" )
        val connected = StubMasterDeviceDescriptor( "Connected" )
        protocol.addMasterDevice( master )
        protocol.addConnectedDevice( connected, master )
        val deployment: StudyDeployment = studyDeploymentFor( protocol )

        val registration = DefaultDeviceRegistration( "0" )
        deployment.registerDevice( master, registration )

        assertFailsWith<IllegalArgumentException>
        {
            deployment.registerDevice( connected, registration )
        }
    }

    @Test
    fun can_registerDevice_with_same_id_for_two_different_unknown_types()
    {
        val protocol = createEmptyProtocol()
        val master = StubMasterDeviceDescriptor()
        val device1 = StubMasterDeviceDescriptor( "Unknown device 1" )
        val device2 = StubDeviceDescriptor( "Unknown device 2" )

        // Mimic that the types are unknown at runtime. When this occurs, they are wrapped in 'Custom...'.
        val device1Custom = CustomDeviceDescriptor( "One class", JSON.encodeToString( StubMasterDeviceDescriptor.serializer(), device1 ), JSON )
        val device2Custom = CustomDeviceDescriptor( "Not the same class", JSON.encodeToString( StubDeviceDescriptor.serializer(), device2 ), JSON )

        protocol.addMasterDevice( master )
        protocol.addConnectedDevice( device1Custom, master )
        protocol.addConnectedDevice( device2Custom, master )
        val deployment: StudyDeployment = studyDeploymentFor( protocol )

        // Even though these two devices are registered using the same ID, this should succeed since they are of different types.
        deployment.registerDevice( device1Custom, DefaultDeviceRegistration( "0" ) )
        deployment.registerDevice( device2Custom, DefaultDeviceRegistration( "0" ) )
    }

    @Test
    fun cant_registerDevice_with_wrong_registration_type()
    {
        val protocol = createEmptyProtocol()
        val master = StubMasterDeviceDescriptor( "Master" )
        protocol.addMasterDevice( master )
        val deployment: StudyDeployment = studyDeploymentFor( protocol )

        val wrongRegistration = AltBeaconDeviceRegistration( 0, UUID.randomUUID(), 0, 0, 0 )
        assertFailsWith<IllegalArgumentException>
        {
            deployment.registerDevice( master, wrongRegistration )
        }
        assertEquals( 0, deployment.consumeEvents().filterIsInstance<StudyDeployment.Event.DeviceRegistered>().count() )
    }

    @Test
    fun unregisterDevice_with_single_device_succeeds()
    {
        val protocol = createEmptyProtocol()
        val device = StubMasterDeviceDescriptor()
        protocol.addMasterDevice( device )
        val deployment: StudyDeployment = studyDeploymentFor( protocol )
        val registration = DefaultDeviceRegistration( "0" )
        deployment.registerDevice( device, registration )

        deployment.unregisterDevice( device )
        assertEquals( 0, deployment.registeredDevices.size )
        assertEquals( 1, deployment.deviceRegistrationHistory[ device ]?.count() )
        assertEquals( registration, deployment.deviceRegistrationHistory[ device ]?.last() )
        assertEquals( StudyDeployment.Event.DeviceUnregistered( device ), deployment.consumeEvents().last() )
        assertTrue( deployment.getStatus().getDeviceStatus( device ) is DeviceDeploymentStatus.Unregistered )
    }

    @Test
    fun unregisterDevice_invalidates_dependent_deployments()
    {
        val protocol = createEmptyProtocol()
        val master1 = StubMasterDeviceDescriptor( "Master 1" )
        protocol.addMasterDevice( master1 )
        val master2 = StubMasterDeviceDescriptor( "Master 2" )
        protocol.addMasterDevice( master2 )
        // TODO: For now, there is no dependency between these two devices, it is simply assumed in the current implementation.
        //       This test will fail once this implementation is improved.
        val deployment = studyDeploymentFor( protocol )
        deployment.registerDevice( master1, master1.createRegistration { } )
        deployment.registerDevice( master2, master2.createRegistration { } )
        val deviceDeployment = deployment.getDeviceDeploymentFor( master1 )
        deployment.deviceDeployed( master1, deviceDeployment.lastUpdatedOn )

        deployment.unregisterDevice( master2 )
        assertEquals( 0, deployment.deployedDevices.count() )
        assertEquals( setOf( master1 ), deployment.invalidatedDeployedDevices )
        val studyStatus = deployment.getStatus()
        assertTrue( studyStatus is StudyDeploymentStatus.DeployingDevices )
        val master1Status = studyStatus.getDeviceStatus( master1 )
        assertTrue( master1Status is DeviceDeploymentStatus.NeedsRedeployment )
        assertEquals( StudyDeployment.Event.DeploymentInvalidated( master1 ), deployment.consumeEvents().last() )
    }

    @Test
    fun unregisterDevice_fails_for_device_not_part_of_deployment()
    {
        val protocol = createSingleMasterWithConnectedDeviceProtocol()
        val deployment: StudyDeployment = studyDeploymentFor( protocol )
        val master = protocol.devices.first { it is AnyMasterDeviceDescriptor }

        assertFailsWith<IllegalArgumentException> { deployment.unregisterDevice( master ) }
    }

    @Test
    fun unregisterDevice_fails_for_device_which_is_not_registered()
    {
        val protocol = createSingleMasterWithConnectedDeviceProtocol()
        val deployment: StudyDeployment = studyDeploymentFor( protocol )

        val invalidDevice = StubMasterDeviceDescriptor( "Not part of deployment" )
        assertFailsWith<IllegalArgumentException> { deployment.unregisterDevice( invalidDevice ) }
    }

    @Test
    fun creating_deployment_fromSnapshot_obtained_by_getSnapshot_is_the_same()
    {
        val deployment = createComplexDeployment()

        val snapshot: StudyDeploymentSnapshot = deployment.getSnapshot()
        val fromSnapshot = StudyDeployment.fromSnapshot( snapshot )

        assertEquals( deployment.id, fromSnapshot.id )
        assertEquals( deployment.createdOn, fromSnapshot.createdOn )
        assertEquals( deployment.protocolSnapshot, fromSnapshot.protocolSnapshot )
        assertEquals(
            deployment.registrableDevices.count(),
            deployment.registrableDevices.intersect( fromSnapshot.registrableDevices ).count() )
        assertEquals(
            deployment.registeredDevices.count(),
            deployment.registeredDevices.entries.intersect( fromSnapshot.registeredDevices.entries ).count() )
        assertEquals(
            deployment.deviceRegistrationHistory.count(),
            deployment.deviceRegistrationHistory.entries.intersect( fromSnapshot.deviceRegistrationHistory.entries ).count() )
        assertEquals(
            deployment.deployedDevices.count(),
            deployment.deployedDevices.intersect( fromSnapshot.deployedDevices ).count() )
        assertEquals(
            deployment.invalidatedDeployedDevices.count(),
            deployment.invalidatedDeployedDevices.intersect( fromSnapshot.invalidatedDeployedDevices ).count() )
        assertEquals( deployment.startedOn, fromSnapshot.startedOn )
        assertEquals( deployment.isStopped, fromSnapshot.isStopped )
        assertEquals( 0, fromSnapshot.consumeEvents().size )
    }

    @Test
    fun fromSnapshot_succeeds_with_rich_registration_history()
    {
        val deployment: StudyDeployment = createActiveDeployment( "Master" )
        val master: AnyMasterDeviceDescriptor = deployment.protocol.devices.first { it.roleName == "Master" } as AnyMasterDeviceDescriptor

        // Create registration history with two registrations for master.
        val registration1 = master.createRegistration()
        val registration2 = master.createRegistration()
        deployment.registerDevice( master, registration1 )
        deployment.unregisterDevice( master )
        deployment.registerDevice( master, registration2 )

        val snapshot = deployment.getSnapshot()
        val fromSnapshot = StudyDeployment.fromSnapshot( snapshot )
        assertEquals( listOf( registration1, registration2 ), fromSnapshot.deviceRegistrationHistory[ master ] )
    }

    @Test
    fun getStatus_contains_invited_participants()
    {
        val deviceRoleName = "Master"
        val protocol = createSingleMasterDeviceProtocol( deviceRoleName )
        val invitation = ParticipantInvitation(
            UUID.randomUUID(),
            setOf( deviceRoleName ),
            UsernameAccountIdentity( "Test" ),
            StudyInvitation( "Test " )
        )
        val deployment = StudyDeployment.fromInvitations( protocol.getSnapshot(), listOf( invitation ) )

        val status = deployment.getStatus()
        assertEquals(
            listOf( ParticipantStatus( invitation.participantId, invitation.assignedMasterDeviceRoleNames ) ),
            status.participantsStatus
        )
    }

    @Test
    fun getStatus_lifecycle_master_and_connected()
    {
        val protocol = createSingleMasterWithConnectedDeviceProtocol( "Master", "Connected" )
        val master = protocol.devices.first { it.roleName == "Master" } as AnyMasterDeviceDescriptor
        val connected = protocol.devices.first { it.roleName == "Connected" }
        val deployment: StudyDeployment = studyDeploymentFor( protocol )

        // Start of deployment, no devices registered.
        val status: StudyDeploymentStatus = deployment.getStatus()
        assertEquals( deployment.id, status.studyDeploymentId )
        assertEquals( 2, status.devicesStatus.count() )
        assertTrue { status.devicesStatus.any { it.device == master } }
        assertTrue { status.devicesStatus.any { it.device == connected } }
        assertTrue( status is StudyDeploymentStatus.Invited )
        val toRegister = status.getRemainingDevicesToRegister()
        val expectedToRegister = setOf<AnyDeviceDescriptor>( master, connected )
        assertEquals( expectedToRegister, toRegister )
        assertTrue( status.getRemainingDevicesReadyToDeploy().isEmpty() )

        // After registering master device, master device is ready for deployment.
        deployment.registerDevice( master, master.createRegistration() )
        val afterMasterRegistered = deployment.getStatus()
        assertTrue( afterMasterRegistered is StudyDeploymentStatus.DeployingDevices )
        assertEquals( 1, afterMasterRegistered.getRemainingDevicesToRegister().count() )
        assertEquals( setOf( master), afterMasterRegistered.getRemainingDevicesReadyToDeploy() )

        // After registering connected device, no more devices need to be registered.
        deployment.registerDevice( connected, connected.createRegistration() )
        val afterAllRegisterSed = deployment.getStatus()
        assertTrue( afterAllRegisterSed is StudyDeploymentStatus.DeployingDevices )
        assertEquals( 0, afterAllRegisterSed.getRemainingDevicesToRegister().count() )
        assertEquals( setOf( master ), afterAllRegisterSed.getRemainingDevicesReadyToDeploy() )

        // Notify of successful master device deployment.
        val deviceDeployment = deployment.getDeviceDeploymentFor( master )
        deployment.deviceDeployed( master, deviceDeployment.lastUpdatedOn )
        val afterDeployStatus = deployment.getStatus()
        assertTrue( afterDeployStatus is StudyDeploymentStatus.Running )
        val deviceStatus = afterDeployStatus.getDeviceStatus( master )
        assertTrue( deviceStatus is DeviceDeploymentStatus.Deployed )
        assertEquals( 0, afterDeployStatus.getRemainingDevicesReadyToDeploy().count() )
    }

    @Test
    fun getStatus_lifecycle_two_dependent_masters()
    {
        val protocol = createEmptyProtocol()
        val master1 = StubMasterDeviceDescriptor( "Master 1" )
        val master2 = StubMasterDeviceDescriptor( "Master 2" )
        protocol.addMasterDevice( master1 )
        protocol.addMasterDevice( master2 )
        // TODO: For now, there is no dependency between these two devices, it is simply assumed in the current implementation.
        //       This test will fail once this implementation is improved.
        val deployment = studyDeploymentFor( protocol )
        deployment.registerDevice( master1, master1.createRegistration() )
        deployment.registerDevice( master2, master2.createRegistration() )

        // Deploy first master device.
        val master1Deployment = deployment.getDeviceDeploymentFor( master1 )
        deployment.deviceDeployed( master1, master1Deployment.lastUpdatedOn )
        assertTrue( deployment.getStatus() is StudyDeploymentStatus.DeployingDevices )

        // After deployment of the second master device, deployment is running.
        val master2Deployment = deployment.getDeviceDeploymentFor( master2 )
        deployment.deviceDeployed( master2, master2Deployment.lastUpdatedOn )
        assertTrue( deployment.getStatus() is StudyDeploymentStatus.Running )

        // Unregistering one device returns deployment to 'deploying'.
        deployment.unregisterDevice( master1 )
        assertTrue( deployment.getStatus() is StudyDeploymentStatus.DeployingDevices )
    }

    @Test
    fun getStatus_for_protocol_with_only_optional_devices()
    {
        val protocol = createEmptyProtocol()
        val master = StubMasterDeviceDescriptor( "Master", isOptional = true )
        val master2 = StubMasterDeviceDescriptor( "Master 2", isOptional = true )
        protocol.addMasterDevice( master )
        protocol.addMasterDevice( master2 )
        val deployment = studyDeploymentFor( protocol )

        // At least one device needs to be deployed before deployment can be considered "Running".
        var status = deployment.getStatus()
        assertTrue( status is StudyDeploymentStatus.Invited )

        // Register device.
        deployment.registerDevice( master, master.createRegistration() )
        status = deployment.getStatus()
        assertTrue( status is StudyDeploymentStatus.DeployingDevices )

        // Deploy device. All other devices are optional, so deployment is "Running".
        val deviceDeployment = deployment.getDeviceDeploymentFor( master )
        deployment.deviceDeployed( master, deviceDeployment.lastUpdatedOn )
        status = deployment.getStatus()
        assertTrue( status is StudyDeploymentStatus.Running )
    }

    @Test
    fun chained_master_devices_cant_be_deployed()
    {
        val protocol = createEmptyProtocol()
        val master = StubMasterDeviceDescriptor( "Master" )
        val chained = StubMasterDeviceDescriptor( "Chained master" )
        protocol.addMasterDevice( master )
        protocol.addConnectedDevice( chained, master )
        val deployment: StudyDeployment = studyDeploymentFor( protocol )

        val status: StudyDeploymentStatus = deployment.getStatus()
        val chainedStatus = status.getDeviceStatus( chained )
        assertFalse { chainedStatus.canBeDeployed }
    }

    @Test
    fun getDeviceDeploymentFor_succeeds()
    {
        val protocol = createSingleMasterWithConnectedDeviceProtocol( "Master", "Connected" )
        protocol.applicationData = "some data"
        val master = protocol.masterDevices.first { it.roleName == "Master" }
        val connected = protocol.devices.first { it.roleName == "Connected" }
        val masterTask = StubTaskDescriptor( "Master task" )
        val connectedTask = StubTaskDescriptor( "Connected task" )
        protocol.addTaskControl( master.atStartOfStudy().start( masterTask, master ) )
        protocol.addTaskControl( master.atStartOfStudy().start( connectedTask, connected ) )
        val deployment = studyDeploymentFor( protocol )
        val registration = DefaultDeviceRegistration( "Registered master" )
        deployment.registerDevice( master, registration )
        deployment.registerDevice( connected, connected.createRegistration() )

        // Later changes made to the protocol don't impact the previously created deployment.
        val ignoredMaster = StubMasterDeviceDescriptor( "Ignored" )
        protocol.addMasterDevice( ignoredMaster ) // Normally, this dependent device would block obtaining deployment.

        val deviceDeployment: MasterDeviceDeployment = deployment.getDeviceDeploymentFor( master )
        assertEquals( "Registered master", deviceDeployment.registration.deviceId )
        assertEquals( protocol.getConnectedDevices( master ).toSet(), deviceDeployment.connectedDevices )
        assertEquals( 1, deviceDeployment.connectedDeviceRegistrations.count() )
        assertEquals( protocol.applicationData, deviceDeployment.applicationData )

        // Device deployment lists both tasks, even if one is destined for the connected device.
        assertEquals( protocol.tasks.count(), deviceDeployment.tasks.intersect( protocol.tasks ).count() )

        // Device deployment contains correct trigger information.
        assertEquals(1, deviceDeployment.triggers.count() )
        assertEquals( master.atStartOfStudy(), deviceDeployment.triggers[ 0 ] )
        val taskControls = deviceDeployment.taskControls
        assertEquals(2, taskControls.count() )
        assertTrue( TaskControl( 0, masterTask.name, master.roleName, TaskControl.Control.Start ) in taskControls )
        assertTrue( TaskControl( 0, connectedTask.name, connected.roleName, TaskControl.Control.Start ) in taskControls )
    }

    @Test
    fun getDeviceDeploymentFor_with_preregistered_device_succeeds()
    {
        val protocol = createSingleMasterWithConnectedDeviceProtocol( "Master", "Connected" )
        val master = protocol.masterDevices.first { it.roleName == "Master" }
        val connected = protocol.devices.first { it.roleName == "Connected" }
        val deployment = studyDeploymentFor( protocol )
        deployment.registerDevice( master, DefaultDeviceRegistration( "0" ) )

        deployment.registerDevice( connected, DefaultDeviceRegistration( "42" ) )
        val deviceDeployment = deployment.getDeviceDeploymentFor( master )

        assertEquals( "Connected", deviceDeployment.connectedDeviceRegistrations.keys.single() )
        assertEquals( "42", deviceDeployment.connectedDeviceRegistrations.getValue( "Connected" ).deviceId )
    }

    @Test
    fun getDeviceDeploymentFor_without_preregistered_device_succeeds()
    {
        val protocol = createSingleMasterWithConnectedDeviceProtocol( "Master", "Connected" )
        val master = protocol.masterDevices.first { it.roleName == "Master" }
        val deployment = studyDeploymentFor( protocol )
        deployment.registerDevice( master, DefaultDeviceRegistration( "0" ) )

        val deviceDeployment = deployment.getDeviceDeploymentFor( master )

        assertTrue( deviceDeployment.connectedDeviceRegistrations.isEmpty() )
    }

    @Test
    fun getDeviceDeploymentFor_with_trigger_to_other_master_device_succeeds()
    {
        val sourceMaster = StubMasterDeviceDescriptor( "Master 1" )
        val targetMaster = StubMasterDeviceDescriptor( "Master 2" )
        val protocol = createEmptyProtocol().apply {
            addMasterDevice( sourceMaster )
            addMasterDevice( targetMaster )
        }
        val measure = Measure.DataStream( DataType( "namespace", "type" ) )
        val task = StubTaskDescriptor( "Stub task", listOf( measure ) )
        protocol.addTaskControl( StubTrigger( sourceMaster ).start( task, targetMaster ) )
        val deployment = studyDeploymentFor( protocol )
        deployment.registerDevice( sourceMaster, DefaultDeviceRegistration( "0" ) )
        deployment.registerDevice( targetMaster, DefaultDeviceRegistration( "1" ) )

        val sourceDeployment = deployment.getDeviceDeploymentFor( sourceMaster )
        val targetDeployment = deployment.getDeviceDeploymentFor( targetMaster )

        // The task should only be run on the target device.
        assertEquals( 0, sourceDeployment.tasks.size )
        assertEquals( task, targetDeployment.tasks.single() )

        // The task is triggered from the source and sent to the target.
        assertEquals( 1, sourceDeployment.triggers.size )
        assertEquals( 1, sourceDeployment.taskControls.size )
        val control = sourceDeployment.taskControls.single()
        assertEquals( task.name, control.taskName )
        assertEquals( 0, targetDeployment.triggers.size )

        // There are no connected devices, only master devices.
        assertEquals( 0, sourceDeployment.connectedDevices.size )
        assertEquals( 0, targetDeployment.connectedDevices.size )
    }

    @Test
    fun getDeviceDeploymentFor_and_deviceDeployed_succeed_with_optional_unregistered_dependent_device()
    {
        val master = StubMasterDeviceDescriptor( "Master 1" )
        val optionalMaster = StubMasterDeviceDescriptor( "Master 2", true )
        val protocol = createEmptyProtocol().apply {
            addMasterDevice( master )
            addMasterDevice( optionalMaster )
        }
        val deployment = studyDeploymentFor( protocol )
        deployment.registerDevice( master, master.createRegistration() )

        // Can get deployment.
        val deploymentStatus = deployment.getStatus()
        val deviceStatus = deploymentStatus.getDeviceStatus( master )
        assertTrue( deviceStatus is DeviceDeploymentStatus.Registered )
        assertTrue( deviceStatus.canObtainDeviceDeployment )
        val deviceDeployment = deployment.getDeviceDeploymentFor( master )
        assertEquals( master, deviceDeployment.deviceDescriptor )

        // Can complete deployment.
        deployment.deviceDeployed( master, deviceDeployment.lastUpdatedOn )
        val status = deployment.getStatus()
        assertTrue( status is StudyDeploymentStatus.Running )
    }

    @Test
    fun getDeviceDeploymentFor_fails_when_device_not_in_protocol()
    {
        val protocol = createSingleMasterWithConnectedDeviceProtocol()
        val deployment = studyDeploymentFor( protocol )

        assertFailsWith<IllegalArgumentException>
        {
            deployment.getDeviceDeploymentFor( StubMasterDeviceDescriptor( "Some other device" ) )
        }
    }

    @Test
    fun getDeviceDeploymentFor_fails_when_device_cant_be_deployed_yet()
    {
        val protocol = createSingleMasterWithConnectedDeviceProtocol( "Master", "Connected" )
        val master = protocol.masterDevices.first { it.roleName == "Master" }
        val deployment = studyDeploymentFor( protocol )

        assertFailsWith<IllegalStateException> { deployment.getDeviceDeploymentFor( master ) }
    }

    @Test
    fun deviceDeployed_succeeds()
    {
        val protocol = createEmptyProtocol()
        val device = StubMasterDeviceDescriptor()
        protocol.addMasterDevice( device )
        val deployment: StudyDeployment = studyDeploymentFor( protocol )
        deployment.registerDevice( device, device.createRegistration { } )

        val deviceDeployment = deployment.getDeviceDeploymentFor( device )
        deployment.deviceDeployed( device, deviceDeployment.lastUpdatedOn )
        assertTrue( deployment.deployedDevices.contains( device ) )
        assertEquals(
            StudyDeployment.Event.DeviceDeployed( device ),
            deployment.consumeEvents().filterIsInstance<StudyDeployment.Event.DeviceDeployed>().singleOrNull() )
    }

    @Test
    fun deviceDeployed_for_last_device_sets_startedOn()
    {
        val protocol = createEmptyProtocol()
        val master1 = StubMasterDeviceDescriptor( "Master1" )
        val master2 = StubMasterDeviceDescriptor( "Master2" )
        protocol.addMasterDevice( master1 )
        protocol.addMasterDevice( master2 )
        val deployment: StudyDeployment = studyDeploymentFor( protocol )
        deployment.registerDevice( master1, master1.createRegistration() )
        deployment.registerDevice( master2, master2.createRegistration() )

        // Deploying a device while others still need to be deployed does not set `startedOn`.
        val master1Deployment = deployment.getDeviceDeploymentFor( master1 )
        deployment.deviceDeployed( master1, master1Deployment.lastUpdatedOn )
        assertNull( deployment.startedOn )
        assertEquals( 0, deployment.consumeEvents().filterIsInstance<StudyDeployment.Event.Started>().count() )

        // Deploying the last device sets `startedOn`.
        val master2Deployment = deployment.getDeviceDeploymentFor( master2 )
        deployment.deviceDeployed( master2, master2Deployment.lastUpdatedOn )
        assertNotNull( deployment.startedOn )
        assertEquals(
            deployment.startedOn,
            deployment.consumeEvents().filterIsInstance<StudyDeployment.Event.Started>().first().startedOn )
    }

    @Test
    fun deviceDeployed_can_be_called_multiple_times()
    {
        val protocol = createEmptyProtocol()
        val device = StubMasterDeviceDescriptor()
        protocol.addMasterDevice( device )
        val deployment: StudyDeployment = studyDeploymentFor( protocol )
        deployment.registerDevice( device, device.createRegistration { } )

        val deviceDeployment = deployment.getDeviceDeploymentFor( device )
        val lastUpdatedOn = deviceDeployment.lastUpdatedOn
        deployment.deviceDeployed( device, lastUpdatedOn )
        deployment.deviceDeployed( device, lastUpdatedOn )
        assertEquals( 1, deployment.deployedDevices.count() )
        assertEquals( 1, deployment.consumeEvents().filterIsInstance<StudyDeployment.Event.DeviceDeployed>().count() )
    }

    @Test
    fun deviceDeployed_fails_for_device_not_part_of_deployment()
    {
        val deployment = createComplexDeployment()

        val invalidDevice = StubMasterDeviceDescriptor( "Not in deployment" )
        assertFailsWith<IllegalArgumentException> { deployment.deviceDeployed( invalidDevice, Clock.System.now() ) }
        assertEquals( 0, deployment.consumeEvents().filterIsInstance<StudyDeployment.Event.DeviceDeployed>().count() )
    }

    @Test
    fun deviceDeployed_fails_when_device_is_unregistered()
    {
        val protocol = createEmptyProtocol()
        val device = StubMasterDeviceDescriptor()
        protocol.addMasterDevice( device )
        val deployment: StudyDeployment = studyDeploymentFor( protocol )

        assertFailsWith<IllegalStateException> { deployment.deviceDeployed( device, Clock.System.now() ) }
        assertEquals( 0, deployment.consumeEvents().filterIsInstance<StudyDeployment.Event.DeviceDeployed>().count() )
    }

    @Test
    fun deviceDeployed_fails_when_connected_device_is_unregistered()
    {
        val protocol = createSingleMasterWithConnectedDeviceProtocol( "Master", "Connected" )
        val master = protocol.masterDevices.first { it.roleName == "Master" }
        val deployment = studyDeploymentFor( protocol )
        deployment.registerDevice( master, DefaultDeviceRegistration( "0" ) )
        val deviceDeployment = deployment.getDeviceDeploymentFor( master )

        assertFailsWith<IllegalStateException> { deployment.deviceDeployed( master, deviceDeployment.lastUpdatedOn ) }
    }

    @Test
    fun deviceDeployed_fails_with_outdated_deployment()
    {
        val protocol = createEmptyProtocol()
        val device = StubMasterDeviceDescriptor()
        protocol.addMasterDevice( device )
        val deployment: StudyDeployment = studyDeploymentFor( protocol )
        deployment.registerDevice( device, device.createRegistration { } )

        val deviceDeployment = deployment.getDeviceDeploymentFor( device )
        deployment.unregisterDevice( device )

        // Ensure new registration is more recent than previous one.
        // The timer precision on the JS runtime is sometimes not enough to notice a difference.
        // In practice, in a distributed system, the timestamps of a re-registration will never overlap due to latency.
        while ( Clock.System.now() == deviceDeployment.lastUpdatedOn ) { /* Wait. */ }
        deployment.registerDevice( device, device.createRegistration { } )

        assertFailsWith<IllegalArgumentException> { deployment.deviceDeployed( device, deviceDeployment.lastUpdatedOn ) }
    }

    @Test
    fun stop_after_ready_succeeds()
    {
        val protocol = createEmptyProtocol()
        val device = StubMasterDeviceDescriptor()
        protocol.addMasterDevice( device )
        val deployment = studyDeploymentFor( protocol )
        deployment.registerDevice( device, device.createRegistration() )
        val deviceDeployment = deployment.getDeviceDeploymentFor( device )
        deployment.deviceDeployed( device, deviceDeployment.lastUpdatedOn )

        assertTrue( deployment.getStatus() is StudyDeploymentStatus.Running )
        assertNull( deployment.stoppedOn )

        deployment.stop()
        assertTrue( deployment.isStopped )
        assertNotNull( deployment.stoppedOn )
        assertTrue( deployment.getStatus() is StudyDeploymentStatus.Stopped )
        assertEquals( 1, deployment.consumeEvents().filterIsInstance<StudyDeployment.Event.Stopped>().count() )
    }

    @Test
    fun stop_while_deploying_succeeds()
    {
        val protocol = createEmptyProtocol()
        val device = StubMasterDeviceDescriptor()
        protocol.addMasterDevice( device )
        val deployment = studyDeploymentFor( protocol )
        deployment.registerDevice( device, device.createRegistration() )

        assertTrue( deployment.getStatus() is StudyDeploymentStatus.DeployingDevices )
        assertNull( deployment.stoppedOn )

        deployment.stop()
        assertTrue( deployment.isStopped )
        assertNotNull( deployment.stoppedOn )
        assertTrue( deployment.getStatus() is StudyDeploymentStatus.Stopped )
        assertEquals( 1, deployment.consumeEvents().filterIsInstance<StudyDeployment.Event.Stopped>().count() )
    }

    @Test
    fun modifications_after_stop_not_allowed()
    {
        val protocol = createSingleMasterWithConnectedDeviceProtocol( "Master", "Connected" )
        val master = protocol.masterDevices.first { it.roleName == "Master" }
        val connected = protocol.devices.first { it.roleName == "Connected" }
        val deployment = studyDeploymentFor( protocol )
        deployment.registerDevice( master, master.createRegistration() )
        deployment.registerDevice( connected, connected.createRegistration() )
        deployment.stop()

        assertFailsWith<IllegalStateException> { deployment.registerDevice( connected, connected.createRegistration() ) }
        assertFailsWith<IllegalStateException> { deployment.unregisterDevice( master ) }
        val deviceDeployment = deployment.getDeviceDeploymentFor( master )
        assertFailsWith<IllegalStateException> { deployment.deviceDeployed( master, deviceDeployment.lastUpdatedOn ) }
    }
}
