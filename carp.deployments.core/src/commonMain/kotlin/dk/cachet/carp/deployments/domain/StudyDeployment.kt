package dk.cachet.carp.deployments.domain

import dk.cachet.carp.common.application.UUID
import dk.cachet.carp.common.application.devices.AnyDeviceConfiguration
import dk.cachet.carp.common.application.devices.AnyPrimaryDeviceConfiguration
import dk.cachet.carp.common.application.devices.DeviceRegistration
import dk.cachet.carp.common.application.devices.isPrimary
import dk.cachet.carp.common.application.tasks.getAllExpectedDataTypes
import dk.cachet.carp.common.application.triggers.TaskControl
import dk.cachet.carp.common.domain.AggregateRoot
import dk.cachet.carp.common.domain.DomainEvent
import dk.cachet.carp.common.infrastructure.serialization.UnknownPolymorphicWrapper
import dk.cachet.carp.data.application.DataStreamsConfiguration
import dk.cachet.carp.deployments.application.DeviceDeploymentStatus
import dk.cachet.carp.deployments.application.PrimaryDeviceDeployment
import dk.cachet.carp.deployments.application.StudyDeploymentStatus
import dk.cachet.carp.deployments.application.throwIfInvalidInvitations
import dk.cachet.carp.deployments.application.users.ParticipantInvitation
import dk.cachet.carp.deployments.application.users.ParticipantStatus
import dk.cachet.carp.deployments.domain.users.getAssignedDeviceRoleNames
import dk.cachet.carp.protocols.application.StudyProtocolSnapshot
import dk.cachet.carp.protocols.domain.StudyProtocol
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant


/**
 * A single instantiation of a [StudyProtocol], taking care of common concerns related to devices when 'running' a study.
 *
 * I.e., a [StudyDeployment] is responsible for registering the physical devices described in the [StudyProtocol],
 * enabling a connection between them, tracking device connection issues, and assessing data quality.
 */
class StudyDeployment private constructor(
    val protocolSnapshot: StudyProtocolSnapshot,
    val participants: List<ParticipantStatus>,
    id: UUID = UUID.randomUUID(),
    createdOn: Instant = Clock.System.now()
) : AggregateRoot<StudyDeployment, StudyDeploymentSnapshot, StudyDeployment.Event>( id, createdOn )
{
    sealed class Event : DomainEvent
    {
        data class DeviceRegistered(
            val device: AnyDeviceConfiguration,
            val registration: DeviceRegistration
        ) : Event()
        data class DeviceUnregistered( val device: AnyDeviceConfiguration ) : Event()
        data class DeviceDeployed( val device: AnyPrimaryDeviceConfiguration ) : Event()
        data class Started( val startedOn: Instant ) : Event()
        data class DeploymentInvalidated( val device: AnyPrimaryDeviceConfiguration ) : Event()
        data class Stopped( val stoppedOn: Instant ) : Event()
    }


    companion object Factory
    {
        /**
         * Initialize a deployment for a [protocolSnapshot] for the participants invited as defined by [invitations].
         *
         * @throws IllegalArgumentException if [invitations] don't match the requirements of the protocol.
         */
        fun fromInvitations(
            protocolSnapshot: StudyProtocolSnapshot,
            invitations: List<ParticipantInvitation>,
            id: UUID = UUID.randomUUID(),
            now: Instant = Clock.System.now()
        ): StudyDeployment
        {
            protocolSnapshot.throwIfInvalidInvitations( invitations )
            val participants = invitations.map { invitation ->
                ParticipantStatus(
                    invitation.participantId,
                    invitation.assignedRoles,
                    protocolSnapshot.getAssignedDeviceRoleNames( invitation.assignedRoles )
                )
            }

            return StudyDeployment( protocolSnapshot, participants, id, now )
        }

        fun fromSnapshot( snapshot: StudyDeploymentSnapshot ): StudyDeployment
        {
            val deployment = StudyDeployment(
                snapshot.studyProtocolSnapshot,
                snapshot.participants.toList(),
                snapshot.id,
                snapshot.createdOn
            )
            deployment.startedOn = snapshot.startedOn

            // Replay device registration history.
            snapshot.deviceRegistrationHistory.forEach { (roleName, registrations) ->
                val device = deployment.registrableDevices.map { it.device }.firstOrNull { it.roleName == roleName }
                    ?: throw IllegalArgumentException(
                        "Can't find registered device with role name '$roleName' in snapshot."
                    )
                registrations.forEachIndexed { index, registration ->
                    val isNotFirstOrLast = index in 1 until registrations.size
                    if ( isNotFirstOrLast ) deployment.unregisterDevice( device )
                    deployment.registerDevice( device, registration )
                }

                // In case snapshot indicates the device is currently not registered, unregister it.
                if ( roleName !in snapshot.registeredDevices ) deployment.unregisterDevice( device )
            }

            // Add deployed devices.
            snapshot.deployedDevices.forEach { roleName ->
                val deployedDevice = deployment.protocolSnapshot.primaryDevices.firstOrNull { it.roleName == roleName }
                    ?: throw IllegalArgumentException(
                        "Can't find deployed device with role name '$roleName' in snapshot."
                    )
                deployment._deployedDevices.add( deployedDevice )
            }

            // Add invalidated deployed devices.
            val invalidatedDevices = snapshot.invalidatedDeployedDevices.map { invalidatedRoleName ->
                deployment.protocolSnapshot.primaryDevices.firstOrNull { it.roleName == invalidatedRoleName }
                    ?: throw IllegalArgumentException(
                        "Can't find deployed device with role name '$invalidatedRoleName' in snapshot."
                    )
            }
            deployment._invalidatedDeployedDevices.addAll( invalidatedDevices )

            // In case the deployment has been stopped, stop it.
            if ( snapshot.isStopped ) deployment.stop()

            // Events introduced by loading the snapshot are not relevant to a consumer wanting to persist changes.
            deployment.consumeEvents()
            deployment.wasLoadedFromSnapshot( snapshot )

            return deployment
        }
    }


    val protocol: StudyProtocol =
        try
        {
            StudyProtocol.fromSnapshot( protocolSnapshot )
        }
        catch ( e: IllegalArgumentException )
        {
            throw IllegalArgumentException( "Invalid protocol snapshot passed.", e )
        }

    /**
     * All the data streams which are required to run this study deployment.
     */
    val requiredDataStreams: DataStreamsConfiguration =
        DataStreamsConfiguration(
            id,
            protocol.devices.flatMap { device ->
                protocol.getTasksForDevice( device )
                    .flatMap { it.getAllExpectedDataTypes() }
                    .map { DataStreamsConfiguration.ExpectedDataStream( device.roleName, it ) }
            }.toSet()
        )

    /**
     * The set of all devices which can or need to be registered for this study deployment.
     */
    val registrableDevices: Set<RegistrableDevice>
        get() = _registrableDevices

    private val _registrableDevices: MutableSet<RegistrableDevice>

    /**
     * The set of devices which are currently registered for this study deployment.
     */
    val registeredDevices: Map<AnyDeviceConfiguration, DeviceRegistration>
        get() = _registeredDevices

    private val _registeredDevices: MutableMap<AnyDeviceConfiguration, DeviceRegistration> = mutableMapOf()

    /**
     * Per device, a list of all device registrations (included old registrations) in the order they were registered.
     */
    val deviceRegistrationHistory: Map<AnyDeviceConfiguration, List<DeviceRegistration>>
        get() = _deviceRegistrationHistory

    private val _deviceRegistrationHistory: MutableMap<AnyDeviceConfiguration, MutableList<DeviceRegistration>> =
        mutableMapOf()

    /**
     * The set of devices which have been deployed correctly.
     */
    val deployedDevices: Set<AnyPrimaryDeviceConfiguration>
        get() = _deployedDevices

    private val _deployedDevices: MutableSet<AnyPrimaryDeviceConfiguration> = mutableSetOf()

    /**
     * Devices which have been previously deployed correctly, but due to changes in device registrations need to be redeployed.
     */
    val invalidatedDeployedDevices: Set<AnyPrimaryDeviceConfiguration>
        get() = _invalidatedDeployedDevices

    private val _invalidatedDeployedDevices: MutableSet<AnyPrimaryDeviceConfiguration> = mutableSetOf()

    /**
     * The time when the study deployment was ready for the first time (all necessary devices deployed);
     * null if the study deployment hasn't started yet.
     */
    var startedOn: Instant? = null
        private set

    /**
     * The time when the study deployment was stopped; null if [isStopped] is false.
     */
    var stoppedOn: Instant? = null

    /**
     * Determines whether the study deployment has been stopped and no further modifications are allowed.
     */
    val isStopped: Boolean
        get() = stoppedOn != null

    init
    {
        require( protocol.isDeployable() ) { "The passed protocol snapshot contains deployment errors." }

        // Initialize information which devices can be registered, deployed, and should be deployed for this deployment.
        _registrableDevices = protocol.devices
            // Top-level primary devices that aren't optional require deployment.
            .map {
                val canBeDeployed = it in protocol.primaryDevices
                val requiresDeployment = canBeDeployed && !it.isOptional
                RegistrableDevice( it, canBeDeployed, requiresDeployment )
            }
            .toMutableSet()
    }


    /**
     * Get the status (serializable) of this [StudyDeployment].
     */
    fun getStatus(): StudyDeploymentStatus
    {
        val devices: Map<RegistrableDevice, DeviceDeploymentStatus> =
            _registrableDevices.associateWith { getDeviceStatus( it.device ) }
        val participantList = participants.toList()
        val allRequiredDevicesDeployed: Boolean = devices
            .filter { it.key.requiresDeployment }
            .all { it.value is DeviceDeploymentStatus.Deployed } &&
                // At least one device needs to be deployed.
                devices.any { it.value is DeviceDeploymentStatus.Deployed }
        val anyRegistration: Boolean = deviceRegistrationHistory.any()

        val deviceList = devices.values.toList()
        return when {
            isStopped ->
                StudyDeploymentStatus.Stopped( createdOn, id, deviceList, participantList, startedOn, stoppedOn!! )
            allRequiredDevicesDeployed ->
                StudyDeploymentStatus.Running( createdOn, id, deviceList, participantList, startedOn!! )
            anyRegistration ->
                StudyDeploymentStatus.DeployingDevices( createdOn, id, deviceList, participantList, startedOn )
            else ->
                StudyDeploymentStatus.Invited( createdOn, id, deviceList, participantList, startedOn )
        }
    }

    /**
     * Get the status of a device in this [StudyDeployment].
     */
    private fun getDeviceStatus( device: AnyDeviceConfiguration ): DeviceDeploymentStatus
    {
        val needsRedeployment = device in invalidatedDeployedDevices
        val isDeployed = device in deployedDevices
        val isRegistered = device in _registeredDevices
        val canBeDeployed = registrableDevices.first{ it.device == device }.canBeDeployed

        val alreadyRegistered = registeredDevices.keys
        val mandatoryDependentDevices = getDependentDevices( device ).filter { !it.isOptional }
        val toRegisterToObtainDeployment = mandatoryDependentDevices
            .plus( device ) // Device itself needs to be registered.
            .minus( alreadyRegistered )
        val mandatoryConnectedDevices =
            if ( device.isPrimary() ) protocol.getConnectedDevices( device ).filter { !it.isOptional }
            else emptyList()
        val toRegisterBeforeDeployment = toRegisterToObtainDeployment
            // Primary devices require non-optional connected devices to be registered.
            .plus( mandatoryConnectedDevices )
            .minus( alreadyRegistered )

        val toObtainDeployment = toRegisterToObtainDeployment.map { it.roleName }.toSet()
        val beforeDeployment = toRegisterBeforeDeployment.map { it.roleName }.toSet()
        return when
        {
            needsRedeployment ->
                DeviceDeploymentStatus.NeedsRedeployment(
                    device,
                    _registeredDevices.getValue( device ),
                    toObtainDeployment,
                    beforeDeployment
                )
            isDeployed ->
                DeviceDeploymentStatus.Deployed(
                    device,
                    _registeredDevices.getValue( device )
                )
            isRegistered ->
                DeviceDeploymentStatus.Registered(
                    device,
                    _registeredDevices.getValue( device ),
                    canBeDeployed,
                    toObtainDeployment,
                    beforeDeployment
                )
            else ->
                DeviceDeploymentStatus.Unregistered( device, canBeDeployed, toObtainDeployment, beforeDeployment )
        }
    }

    /**
     * Get all devices which the passed [device] depends on the registration of.
     */
    private fun getDependentDevices( device: AnyDeviceConfiguration ): List<AnyDeviceConfiguration> =
        when ( device )
        {
            is AnyPrimaryDeviceConfiguration ->
                // TODO: For now, presume all devices which require deployment may depend on one another.
                //       This can be optimized by looking at the triggers which determine actual dependencies between devices.
                _registrableDevices
                    .filter { it.requiresDeployment }
                    .map { it.device }
                    .minus( device )
            else -> emptyList() // Only primary devices can be deployed. Other devices have no 'dependent' devices.
        }

    /**
     * Register the specified [device] for this deployment using the passed [registration] options.
     *
     * @throws IllegalArgumentException when the passed device is not part of this deployment or is already registered.
     * @throws IllegalStateException when this deployment has stopped.
     */
    fun registerDevice( device: AnyDeviceConfiguration, registration: DeviceRegistration )
    {
        val containsDevice: Boolean = _registrableDevices.any { it.device == device }
        require( containsDevice ) { "The passed device is not part of this deployment." }

        check( !isStopped ) { "Cannot register devices after a study deployment has stopped." }

        val isAlreadyRegistered = device in _registeredDevices.keys
        require( !isAlreadyRegistered ) { "The passed device is already registered." }

        // Fail for device registrations which are known to be invalid.
        // Registrations for which this is unknown are simply forwarded, possibly failing on the 'client' later.
        val isInvalidRegistration = device.isDefinitelyInvalidRegistration( registration )
        require( !isInvalidRegistration ) { "The passed registration is not valid for the given device." }

        // Verify whether deviceId is unique for the given device type within this deployment.
        val isUnique = _registeredDevices.none {
            val isSameId = it.value.deviceId == registration.deviceId
            val otherDevice = it.key
            val areUnknownDevices = device is UnknownPolymorphicWrapper && otherDevice is UnknownPolymorphicWrapper
            val matchingUnknownDevices: Boolean by lazy {
                (device as UnknownPolymorphicWrapper).className == (otherDevice as UnknownPolymorphicWrapper).className
            }
            isSameId && (
                (!areUnknownDevices && otherDevice::class == device::class) ||
                (areUnknownDevices && matchingUnknownDevices)
            )
        }
        require( isUnique )
        {
            "The deviceId specified in the passed registration is already in use by a device of the same type. " +
            "Cannot register the same device for different device roles within a deployment."
        }

        // Add device to currently registered devices, but also store it in registration history.
        _registeredDevices[ device ] = registration
        val registrationHistory = _deviceRegistrationHistory.getOrPut( device ) { mutableListOf() }
        registrationHistory.add( registration )
        event( Event.DeviceRegistered( device, registration ) )

        invalidateDeploymentOfDependentDevices( device )
    }

    /**
     * Remove the current device registration for the [device] in this deployment.
     * This will invalidate the deployment of any devices which depend on the this [device].
     *
     * @throws IllegalArgumentException when the passed device is not part of this deployment or is not registered.
     * @throws IllegalStateException when this deployment has stopped.
     */
    fun unregisterDevice( device: AnyDeviceConfiguration )
    {
        val containsDevice: Boolean = _registrableDevices.any { it.device == device }
        require( containsDevice ) { "The passed device is not part of this deployment." }
        require( device in _registeredDevices ) { "The passed device is not registered for this deployment." }

        check( !isStopped ) { "Cannot unregister devices after a study deployment has stopped." }

        _registeredDevices.remove( device )
        _deployedDevices.remove( device )

        event( Event.DeviceUnregistered( device ) )

        invalidateDeploymentOfDependentDevices( device )
    }

    /**
     * Invalidate deployed primary devices which depend on this [device].
     */
    private fun invalidateDeploymentOfDependentDevices( device: AnyDeviceConfiguration )
    {
        val dependentPrimaryDevices = getDependentDevices( device )
            .filterIsInstance<AnyPrimaryDeviceConfiguration>()
        dependentPrimaryDevices.forEach {
            _deployedDevices
                .remove( it )
                .eventIf( true ) {
                    _invalidatedDeployedDevices.add( it )
                    Event.DeploymentInvalidated( it )
                }
        }
    }

    /**
     * Get the deployment configuration for the specified [device] in this study deployment.
     *
     * @throws IllegalArgumentException when the passed [device] is not part of the protocol of this study deployment.
     * @throws IllegalStateException when a [PrimaryDeviceDeployment] for the passed [device] is not yet available.
     */
    fun getDeviceDeploymentFor( device: AnyPrimaryDeviceConfiguration ): PrimaryDeviceDeployment
    {
        // Verify whether the specified device is part of the protocol of this deployment.
        require( device in protocolSnapshot.primaryDevices )
            { "The specified primary device is not part of the protocol of this deployment." }

        // Verify whether the specified device is ready to be deployed.
        val canDeploy = getDeviceStatus( device ).canObtainDeviceDeployment
        check( canDeploy )
            { "The specified device is awaiting registration of itself or other devices before it can be deployed." }

        // Configuration must be non-null, otherwise canObtainDeviceDeployment would fail.
        val configuration: DeviceRegistration = _registeredDevices[ device ]!!

        // Determine which devices this device needs to connect to and retrieve configuration for preregistered devices.
        val connectedDevices: Set<AnyDeviceConfiguration> = protocol.getConnectedDevices( device ).toSet()
        val deviceRegistrations: Map<String, DeviceRegistration> = _registeredDevices
            .filter { it.key in connectedDevices }
            .mapKeys { it.key.roleName }

        // Get all tasks which might need to be executed on this or connected devices.
        val relevantDevices = arrayOf( device ).union( connectedDevices )
        val tasks = relevantDevices
            .flatMap { protocol.getTasksForDevice( it ) }
            .toSet()

        // Get all trigger information for this and connected devices.
        // The trigger IDs assigned by snapshot are reused to identify them within the protocol.
        val relevantDeviceRoles = relevantDevices.map { it.roleName }
        val usedTriggers = protocolSnapshot.triggers
            .filter { it.value.sourceDeviceRoleName in relevantDeviceRoles }
        val taskControls = usedTriggers
            .map { it to protocol.getTaskControls( it.value ) }
            .flatMap { pair ->
                pair.second.map {
                    TaskControl( pair.first.key, it.task.name, it.destinationDevice.roleName, it.control )
                }
            }
            .toSet()

        return PrimaryDeviceDeployment(
            device,
            configuration,
            connectedDevices,
            deviceRegistrations,
            tasks,
            usedTriggers,
            taskControls,
            protocol.expectedParticipantData.toSet(),
            protocol.applicationData
        )
    }

    /**
     * Indicate that the specified [device] was deployed successfully
     * using the device deployment with the timestamp matching [deviceDeploymentLastUpdatedOn].
     *
     * @throws IllegalArgumentException when:
     * - the passed [device] is not part of the protocol of this study deployment
     * - the [deviceDeploymentLastUpdatedOn] does not match the expected timestamp. The deployment might be outdated.
     * @throws IllegalStateException when the passed [device] cannot be deployed yet, or the deployment has stopped.
     */
    fun deviceDeployed( device: AnyPrimaryDeviceConfiguration, deviceDeploymentLastUpdatedOn: Instant )
    {
        // Verify whether the specified device is part of the protocol of this deployment.
        require( device in protocolSnapshot.primaryDevices )
            { "The specified primary device is not part of the protocol of this deployment." }

        // Verify whether deployment matches the expected deployment.
        val latestDeployment = getDeviceDeploymentFor( device )
        require( latestDeployment.lastUpdatedOn == deviceDeploymentLastUpdatedOn )

        check( !isStopped ) { "Cannot deploy devices after a study deployment has stopped." }

        // Verify whether the specified device is ready to be deployed.
        val canDeploy = getDeviceStatus( device ).let {
            it is DeviceDeploymentStatus.Deployed ||
            it is DeviceDeploymentStatus.NotDeployed && it.isReadyForDeployment
        }
        check( canDeploy )
            { "The specified device is awaiting registration of itself or other devices before it can be deployed." }

        _deployedDevices
            .add( device )
            .eventIf( true ) { Event.DeviceDeployed( device ) }

        // Set start time when deployment starts running (all necessary devices deployed).
        val allRequiredDeviceDeployed = _registrableDevices
            .filter { it.requiresDeployment }
            .map { getDeviceStatus( it.device ) }
            .all { it is DeviceDeploymentStatus.Deployed }
        if ( startedOn == null && allRequiredDeviceDeployed )
        {
            val now = Clock.System.now()
            startedOn = now
            event( Event.Started( now ) )
        }
    }

    /**
     * Stop this study deployment.
     * No further changes to this deployment are allowed and no more data should be collected.
     */
    fun stop( now: Instant = Clock.System.now() )
    {
        if ( !isStopped )
        {
            stoppedOn = now
            event( Event.Stopped( now ) )
        }
    }


    /**
     * Get an immutable snapshot of the current state of this [StudyDeployment] using the specified snapshot [version].
     */
    override fun getSnapshot( version: Int ): StudyDeploymentSnapshot =
        StudyDeploymentSnapshot.fromDeployment( this, version )
}
