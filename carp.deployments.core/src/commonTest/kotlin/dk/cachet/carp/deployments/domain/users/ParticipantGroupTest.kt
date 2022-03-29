package dk.cachet.carp.deployments.domain.users

import dk.cachet.carp.common.application.UUID
import dk.cachet.carp.common.application.data.Data
import dk.cachet.carp.common.application.data.input.CarpInputDataTypes
import dk.cachet.carp.common.application.data.input.CustomInput
import dk.cachet.carp.common.application.data.input.InputDataType
import dk.cachet.carp.common.application.data.input.Sex
import dk.cachet.carp.common.application.data.input.elements.Text
import dk.cachet.carp.common.application.users.ParticipantAttribute
import dk.cachet.carp.common.domain.users.Account
import dk.cachet.carp.common.infrastructure.test.StubPrimaryDeviceConfiguration
import dk.cachet.carp.deployments.application.users.AssignedPrimaryDevice
import dk.cachet.carp.deployments.application.users.Participation
import dk.cachet.carp.deployments.application.users.StudyInvitation
import dk.cachet.carp.deployments.domain.createComplexParticipantGroup
import dk.cachet.carp.deployments.domain.studyDeploymentFor
import dk.cachet.carp.protocols.application.users.ExpectedParticipantData
import dk.cachet.carp.protocols.domain.StudyProtocol
import dk.cachet.carp.protocols.infrastructure.test.createSinglePrimaryDeviceProtocol
import kotlin.test.*

/**
 * Tests for [ParticipantGroup].
 */
class ParticipantGroupTest
{
    private val studyInvitation = StudyInvitation( "Some study" )


    @Test
    fun fromNewDeployment_succeeds()
    {
        val protocol: StudyProtocol = createSinglePrimaryDeviceProtocol()
        val expectedDataInputType = InputDataType( "some", "type" )
        val expectedData = ExpectedParticipantData(
            ParticipantAttribute.DefaultParticipantAttribute( expectedDataInputType )
        )
        protocol.addExpectedParticipantData( expectedData )
        val deployment = studyDeploymentFor( protocol )

        val group = ParticipantGroup.fromNewDeployment( deployment )

        assertEquals( deployment.id, group.studyDeploymentId )
        val expectedAssignedPrimaryDevices = protocol.primaryDevices
            .map { AssignedPrimaryDevice( it ) }
            .toSet()
        assertEquals( expectedAssignedPrimaryDevices, group.assignedPrimaryDevices )
        assertEquals( mapOf( expectedDataInputType to null ), group.data )
        assertEquals( 0, group.participations.size )
        assertEquals( 0, group.consumeEvents().size )
    }

    @Test
    fun creating_ParticipantGroup_fromSnapshot_obtained_by_getSnapshot_is_the_same()
    {
        val group = createComplexParticipantGroup()
        val snapshot: ParticipantGroupSnapshot = group.getSnapshot()
        val fromSnapshot = ParticipantGroup.fromSnapshot( snapshot )

        assertEquals( group.createdOn, fromSnapshot.createdOn )
        assertEquals( group.studyDeploymentId, fromSnapshot.studyDeploymentId )
        assertEquals( group.assignedPrimaryDevices, fromSnapshot.assignedPrimaryDevices )
        assertEquals( group.isStudyDeploymentStopped, fromSnapshot.isStudyDeploymentStopped )
        assertEquals( group.participations, fromSnapshot.participations )
        assertEquals( group.expectedData, fromSnapshot.expectedData )
        assertEquals(
            group.data.map { it.key to it.value }.toSet(),
            fromSnapshot.data.map { it.key to it.value }.toSet()
        )
        assertEquals( 0, fromSnapshot.consumeEvents().size )
    }

    @Test
    fun addParticipation_succeeds()
    {
        val group = createParticipantGroup()
        val devicesToAssign = group.assignedPrimaryDevices.map { it.device }.toSet()

        val participation = Participation( group.studyDeploymentId )
        val account = Account.withUsernameIdentity( "test" )
        group.addParticipation( account, studyInvitation, participation, devicesToAssign )

        val expectedParticipation = AccountParticipation(
            participation,
            devicesToAssign.map { it.roleName }.toSet(),
            account.id,
            studyInvitation
        )
        assertEquals( ParticipantGroup.Event.ParticipationAdded( expectedParticipation ), group.consumeEvents().last() )
    }

    @Test
    fun addParticipation_for_incorrect_study_deployment_fails()
    {
        val group = createParticipantGroup()
        val devicesToAssign = group.assignedPrimaryDevices.map { it.device }.toSet()

        val account = Account.withUsernameIdentity( "test" )
        val incorrectDeploymentId = UUID.randomUUID()
        val participation = Participation( incorrectDeploymentId )
        assertFailsWith<IllegalArgumentException>
        {
            group.addParticipation( account, studyInvitation, participation, devicesToAssign )
        }
        assertEquals( 0, group.consumeEvents().filterIsInstance<ParticipantGroup.Event.ParticipationAdded>().count() )
    }

    @Test
    fun addParticipation_for_existing_participation_fails()
    {
        val group = createParticipantGroup()
        val devicesToAssign = group.assignedPrimaryDevices.map { it.device }.toSet()
        val account = Account.withUsernameIdentity( "test" )
        val participation = Participation( group.studyDeploymentId )
        val invitation = studyInvitation
        group.addParticipation( account, invitation, participation, devicesToAssign )

        assertFailsWith<IllegalArgumentException>
        {
            group.addParticipation( account, invitation, participation, devicesToAssign )
        }
        assertEquals( 1, group.consumeEvents().filterIsInstance<ParticipantGroup.Event.ParticipationAdded>().count() )
    }

    @Test
    fun addParticipation_fails_when_deployment_stopped()
    {
        val group = createParticipantGroup()
        val devicesToAssign = group.assignedPrimaryDevices.map { it.device }.toSet()
        group.studyDeploymentStopped()

        val account = Account.withUsernameIdentity( "test" )
        val participation = Participation( group.studyDeploymentId )
        assertFailsWith<IllegalStateException>
        {
            group.addParticipation( account, studyInvitation, participation, devicesToAssign )
        }
    }

    @Test
    fun getAssignedPrimaryDevice_succeeds()
    {
        val primaryDeviceRoleName = "Primary"
        val protocol = createSinglePrimaryDeviceProtocol( primaryDeviceRoleName )
        val group = createParticipantGroup( protocol )

        val assignedDevice = group.getAssignedPrimaryDevice( primaryDeviceRoleName )
        assertEquals( protocol.primaryDevices.single(), assignedDevice.device )
    }

    @Test
    fun getAssignedPrimaryDevice_fails_for_unknown_device()
    {
        val group = createParticipantGroup()

        assertFailsWith<IllegalArgumentException> { group.getAssignedPrimaryDevice( "Unknown" ) }
    }

    @Test
    fun updateDeviceRegistration_succeeds()
    {
        val protocol = createSinglePrimaryDeviceProtocol()
        val device = protocol.primaryDevices.first()
        val group = createParticipantGroup( protocol )

        val registration = device.createRegistration()
        group.updateDeviceRegistration( device, registration )
        val assignedDevice = group.assignedPrimaryDevices.firstOrNull { it.device == device }

        assertEquals( registration, assignedDevice?.registration )
        val registrationEvent = group.consumeEvents().filterIsInstance<ParticipantGroup.Event.DeviceRegistrationChanged>().singleOrNull()
        assertNotNull( registrationEvent )
        assertEquals( AssignedPrimaryDevice( device, registration ), registrationEvent.assignedPrimaryDevice )
    }

    @Test
    fun updateDeviceRegistration_does_not_trigger_event_for_unchanged_registration()
    {
        val protocol = createSinglePrimaryDeviceProtocol()
        val device = protocol.primaryDevices.first()
        val group = createParticipantGroup( protocol )
        val registration = device.createRegistration()
        group.updateDeviceRegistration( device, registration )

        group.consumeEvents()
        group.updateDeviceRegistration( device, registration )
        assertEquals( 0, group.consumeEvents().size )
    }

    @Test
    fun updateDeviceRegistration_fails_for_unknown_device()
    {
        val group = createParticipantGroup()

        val unknownDevice = StubPrimaryDeviceConfiguration()
        assertFailsWith<IllegalArgumentException>
        {
            group.updateDeviceRegistration( unknownDevice, null )
        }
        assertEquals( 0, group.consumeEvents().filterIsInstance<ParticipantGroup.Event.DeviceRegistrationChanged>().count() )
    }

    @Test
    fun studyDeploymentStopped_succeeds()
    {
        val group = createParticipantGroup()

        group.studyDeploymentStopped()

        assertTrue( group.isStudyDeploymentStopped )
        assertEquals( 1, group.consumeEvents().filterIsInstance<ParticipantGroup.Event.StudyDeploymentStopped>().count() )
    }

    @Test
    fun setData_succeeds()
    {
        val protocol: StudyProtocol = createSinglePrimaryDeviceProtocol()
        val expectedData = ExpectedParticipantData(
            ParticipantAttribute.DefaultParticipantAttribute( CarpInputDataTypes.SEX )
        )
        protocol.addExpectedParticipantData( expectedData )

        val group = createParticipantGroup( protocol )

        val isSet = group.setData( CarpInputDataTypes, CarpInputDataTypes.SEX, Sex.Male )
        assertTrue( isSet )
        assertEquals( Sex.Male, group.data[ CarpInputDataTypes.SEX ] )
        assertEquals(
            ParticipantGroup.Event.DataSet( CarpInputDataTypes.SEX, Sex.Male ),
            group.consumeEvents().filterIsInstance<ParticipantGroup.Event.DataSet>().singleOrNull()
        )
    }

    @Test
    fun setData_returns_false_when_data_already_set()
    {
        val protocol: StudyProtocol = createSinglePrimaryDeviceProtocol()
        val expectedData = ExpectedParticipantData(
            ParticipantAttribute.DefaultParticipantAttribute( CarpInputDataTypes.SEX )
        )
        protocol.addExpectedParticipantData( expectedData )

        val group = createParticipantGroup( protocol )
        group.setData( CarpInputDataTypes, CarpInputDataTypes.SEX, Sex.Male )
        group.consumeEvents()

        val isSet = group.setData( CarpInputDataTypes, CarpInputDataTypes.SEX, Sex.Male )
        assertFalse( isSet )
        assertEquals(
            0,
            group.consumeEvents().filterIsInstance<ParticipantGroup.Event.DataSet>().count()
        )
    }

    @Test
    fun setData_fails_for_unexpected_data()
    {
        val group = createParticipantGroup()

        assertFailsWith<IllegalArgumentException>
        {
            group.setData( CarpInputDataTypes, CarpInputDataTypes.SEX, Sex.Male )
        }
    }

    @Test
    fun setData_fails_for_invalid_data()
    {
        val protocol: StudyProtocol = createSinglePrimaryDeviceProtocol()
        val expectedData = ExpectedParticipantData(
             ParticipantAttribute.DefaultParticipantAttribute( CarpInputDataTypes.SEX )
        )
        protocol.addExpectedParticipantData( expectedData )
        val group = createParticipantGroup( protocol )

        val wrongData = object : Data { }
        assertFailsWith<IllegalArgumentException>
        {
            group.setData( CarpInputDataTypes, CarpInputDataTypes.SEX, wrongData )
        }
    }

    @Test
    fun setData_multiple_data_succeeds()
    {
        val protocol: StudyProtocol = createSinglePrimaryDeviceProtocol()
        val defaultExpectedData = ExpectedParticipantData(
            ParticipantAttribute.DefaultParticipantAttribute( CarpInputDataTypes.SEX )
        )
        protocol.addExpectedParticipantData( defaultExpectedData )
        val customExpecteData = ExpectedParticipantData(
            ParticipantAttribute.CustomParticipantAttribute( Text( "Test" ) )
        )
        protocol.addExpectedParticipantData( customExpecteData )

        val group = createParticipantGroup( protocol )

        val toSet = mapOf(
            CarpInputDataTypes.SEX to Sex.Male,
            customExpecteData.inputDataType to CustomInput( "Test" )
        )
        val isSet = group.setData( CarpInputDataTypes, toSet )
        assertTrue( isSet )
        assertEquals( Sex.Male, group.data[ CarpInputDataTypes.SEX ] )
        assertEquals( CustomInput( "Test" ), group.data[ customExpecteData.inputDataType ] )
        assertEquals(
            setOf(
                ParticipantGroup.Event.DataSet( CarpInputDataTypes.SEX, Sex.Male ),
                ParticipantGroup.Event.DataSet( customExpecteData.inputDataType, CustomInput( "Test" ) )
            ),
            group.consumeEvents().filterIsInstance<ParticipantGroup.Event.DataSet>().toSet()
        )
    }

    @Test
    fun setData_multiple_data_returns_false_when_all_data_already_set()
    {
        val protocol: StudyProtocol = createSinglePrimaryDeviceProtocol()
        val defaultExpectedData = ExpectedParticipantData(
            ParticipantAttribute.DefaultParticipantAttribute( CarpInputDataTypes.SEX )
        )
        protocol.addExpectedParticipantData( defaultExpectedData )
        val customExpectedData = ExpectedParticipantData(
            ParticipantAttribute.CustomParticipantAttribute( Text( "Test" ) )
        )
        protocol.addExpectedParticipantData( customExpectedData )
        val group = createParticipantGroup( protocol )
        val toSet = mapOf(
            CarpInputDataTypes.SEX to Sex.Male,
            customExpectedData.inputDataType to CustomInput( "Test" )
        )
        group.setData( CarpInputDataTypes, toSet )
        group.consumeEvents()

        val isSet = group.setData( CarpInputDataTypes, toSet )
        assertFalse( isSet )
        assertEquals(
            0,
            group.consumeEvents().filterIsInstance<ParticipantGroup.Event.DataSet>().count()
        )
    }

    @Test
    fun setData_multiple_data_succeeds_fully_or_fails()
    {
        val protocol: StudyProtocol = createSinglePrimaryDeviceProtocol()
        val customExpectedData = ExpectedParticipantData(
            ParticipantAttribute.CustomParticipantAttribute( Text( "Test" ) )
        )
        protocol.addExpectedParticipantData( customExpectedData )
        val group = createParticipantGroup( protocol )

        val toSet = mapOf(
            customExpectedData.inputDataType to CustomInput( "Test" ),
            CarpInputDataTypes.SEX to Sex.Male // Not expected, thus should fail.
        )
        assertFailsWith<IllegalArgumentException> { group.setData( CarpInputDataTypes, toSet ) }
        assertEquals( null, group.data[ customExpectedData.inputDataType ] )
        assertEquals(
            0,
            group.consumeEvents().filterIsInstance<ParticipantGroup.Event.DataSet>().count()
        )
    }

    private fun createParticipantGroup( protocol: StudyProtocol = createSinglePrimaryDeviceProtocol() ): ParticipantGroup
    {
        val deployment = studyDeploymentFor( protocol )
        return ParticipantGroup.fromNewDeployment( deployment )
    }
}
