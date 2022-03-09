package dk.cachet.carp.deployments.domain

import dk.cachet.carp.common.application.UUID
import dk.cachet.carp.common.application.users.AccountIdentity
import dk.cachet.carp.common.infrastructure.test.StubPrimaryDeviceConfiguration
import dk.cachet.carp.deployments.application.DeploymentService
import dk.cachet.carp.deployments.application.users.ParticipantInvitation
import dk.cachet.carp.deployments.application.users.StudyInvitation
import dk.cachet.carp.deployments.domain.users.AccountService
import dk.cachet.carp.deployments.domain.users.ParticipantGroupService
import dk.cachet.carp.deployments.infrastructure.InMemoryAccountService
import dk.cachet.carp.protocols.domain.StudyProtocol
import dk.cachet.carp.protocols.infrastructure.test.createEmptyProtocol
import dk.cachet.carp.protocols.infrastructure.test.createSinglePrimaryDeviceProtocol
import kotlinx.coroutines.test.runTest
import kotlin.test.*


/**
 * Tests for [ParticipantGroupService].
 */
class ParticipantGroupServiceTest
{
    private lateinit var accountService: AccountService
    private lateinit var service: ParticipantGroupService

    private val protocol: StudyProtocol = createSinglePrimaryDeviceProtocol()
    val studyDeploymentId = UUID.randomUUID()

    @BeforeTest
    fun createServices()
    {
        accountService = InMemoryAccountService()
        service = ParticipantGroupService( accountService )
    }


    @Test
    fun createAndInviteParticipantGroup_has_matching_studyDeploymentId() = runTest {
        val createdEvent = DeploymentService.Event.StudyDeploymentCreated(
            studyDeploymentId,
            protocol.getSnapshot(),
            listOf( createParticipantInvitation( protocol ) ),
            connectedDevicePreregistrations = emptyMap()
        )
        val group = service.createAndInviteParticipantGroup( createdEvent )

        assertEquals( studyDeploymentId, group.studyDeploymentId )
    }

    @Test
    fun createAndInviteParticipantGroup_creates_new_account_for_new_identity() = runTest {
        val emailIdentity = AccountIdentity.fromEmailAddress( "test@test.com" )

        val createdEvent = DeploymentService.Event.StudyDeploymentCreated(
            studyDeploymentId,
            protocol.getSnapshot(),
            listOf( createParticipantInvitation( protocol, emailIdentity ) ),
            connectedDevicePreregistrations = emptyMap()
        )
        service.createAndInviteParticipantGroup( createdEvent )

        // Verify whether account was added.
        val foundAccount = accountService.findAccount( emailIdentity )
        assertNotNull( foundAccount )
    }

    @Test
    fun createAndInviteParticipantGroup_with_multiple_participations_per_account_succeeds() = runTest {
        val device1Role = "Primary 1"
        val device2Role = "Primary 2"
        val protocol = createEmptyProtocol().apply {
            addPrimaryDevice( StubPrimaryDeviceConfiguration( device1Role ) )
            addPrimaryDevice( StubPrimaryDeviceConfiguration( device2Role ) )
        }
        val identity = AccountIdentity.fromUsername( "Test" )
        val studyInvitation = StudyInvitation( "Some study" )
        val invitation1 = ParticipantInvitation( UUID.randomUUID(), setOf( device1Role ), identity, studyInvitation )
        val invitation2 = ParticipantInvitation( UUID.randomUUID(), setOf( device2Role ), identity, studyInvitation )

        val createdEvent = DeploymentService.Event.StudyDeploymentCreated(
            studyDeploymentId,
            protocol.getSnapshot(),
            listOf( invitation1, invitation2 ),
            connectedDevicePreregistrations = emptyMap()
        )
        val group = service.createAndInviteParticipantGroup( createdEvent )
        assertEquals( 2, group.participations.count() )
    }

    @Test
    fun createAndInviteParticipantGroup_fails_for_unknown_deviceRoleNames() = runTest {
        val errorneousInvitation = ParticipantInvitation(
            UUID.randomUUID(),
            setOf( "Wrong device" ),
            AccountIdentity.fromUsername( "Test" ),
            StudyInvitation( "Some study" )
        )
        val createdEvent = DeploymentService.Event.StudyDeploymentCreated(
            studyDeploymentId,
            protocol.getSnapshot(),
            listOf( errorneousInvitation ),
            connectedDevicePreregistrations = emptyMap()
        )

        assertFailsWith<IllegalArgumentException> { service.createAndInviteParticipantGroup( createdEvent ) }
    }
}
