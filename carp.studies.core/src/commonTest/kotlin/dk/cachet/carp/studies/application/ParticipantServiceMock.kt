package dk.cachet.carp.studies.application

import dk.cachet.carp.common.EmailAddress
import dk.cachet.carp.common.UUID
import dk.cachet.carp.common.users.AccountIdentity
import dk.cachet.carp.deployment.domain.StudyDeploymentStatus
import dk.cachet.carp.studies.domain.ParticipantGroupStatus
import dk.cachet.carp.studies.domain.users.AssignParticipantDevices
import dk.cachet.carp.studies.domain.users.Participant
import dk.cachet.carp.test.Mock

// TODO: Due to a bug, `Service` cannot be used here, although that would be preferred.
//       Change this once this is fixed: https://youtrack.jetbrains.com/issue/KT-24700
// private typealias Service = ParticipantService


class ParticipantServiceMock(
    private val addParticipantResult: Participant = Participant( AccountIdentity.fromEmailAddress( "test@test.com" ) ),
    private val getParticipantsResult: List<Participant> = emptyList(),
    private val deployParticipantResult: ParticipantGroupStatus = groupStatus,
    private val getParticipantGroupStatusListResult: List<ParticipantGroupStatus> = emptyList(),
    private val stopParticipantGroupResult: ParticipantGroupStatus = groupStatus
) : Mock<ParticipantService>(), ParticipantService
{
    companion object
    {
        private val groupStatus = ParticipantGroupStatus(
            StudyDeploymentStatus.Invited( UUID.randomUUID(), emptyList(), null ),
            emptySet() )
    }


    override suspend fun addParticipant( studyId: UUID, email: EmailAddress ) =
        addParticipantResult
        .also { trackSuspendCall( ParticipantService::addParticipant, studyId, email ) }

    override suspend fun getParticipants( studyId: UUID ) =
        getParticipantsResult
        .also { trackSuspendCall( ParticipantService::getParticipants, studyId ) }

    override suspend fun deployParticipantGroup( studyId: UUID, group: Set<AssignParticipantDevices> ) =
        deployParticipantResult
        .also { trackSuspendCall( ParticipantService::deployParticipantGroup, studyId, group ) }

    override suspend fun getParticipantGroupStatusList( studyId: UUID ) =
        getParticipantGroupStatusListResult
        .also { trackSuspendCall( ParticipantService::getParticipantGroupStatusList, studyId ) }

    override suspend fun stopParticipantGroup( studyId: UUID, groupId: UUID ) =
        stopParticipantGroupResult
        .also { trackSuspendCall( ParticipantService::stopParticipantGroup, studyId, groupId ) }
}