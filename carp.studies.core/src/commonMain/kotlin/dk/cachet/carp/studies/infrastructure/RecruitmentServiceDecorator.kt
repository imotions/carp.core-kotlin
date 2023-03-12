package dk.cachet.carp.studies.infrastructure

import dk.cachet.carp.common.application.EmailAddress
import dk.cachet.carp.common.application.UUID
import dk.cachet.carp.common.infrastructure.services.ApplicationServiceDecorator
import dk.cachet.carp.common.infrastructure.services.ApplicationServiceInvoker
import dk.cachet.carp.common.infrastructure.services.Command
import dk.cachet.carp.studies.application.RecruitmentService
import dk.cachet.carp.studies.application.users.AssignedParticipantRoles


class RecruitmentServiceDecorator(
    service: RecruitmentService,
    requestDecorator: (Command<RecruitmentServiceRequest<*>>) -> Command<RecruitmentServiceRequest<*>>
) : ApplicationServiceDecorator<RecruitmentService, RecruitmentServiceRequest<*>>(
        service,
        RecruitmentServiceInvoker,
        requestDecorator
    ),
    RecruitmentService
{
    override suspend fun addParticipant( studyId: UUID, email: EmailAddress ) =
        invoke( RecruitmentServiceRequest.AddParticipant( studyId, email ) )

    override suspend fun getParticipant( studyId: UUID, participantId: UUID ) =
        invoke( RecruitmentServiceRequest.GetParticipant( studyId, participantId ) )

    override suspend fun getParticipants( studyId: UUID ) =
        invoke( RecruitmentServiceRequest.GetParticipants( studyId ) )

    override suspend fun inviteNewParticipantGroup(
        studyId: UUID,
        group: Set<AssignedParticipantRoles>
    ) = invoke( RecruitmentServiceRequest.InviteNewParticipantGroup( studyId, group ) )

    override suspend fun getParticipantGroupStatusList( studyId: UUID ) =
        invoke( RecruitmentServiceRequest.GetParticipantGroupStatusList( studyId ) )

    override suspend fun stopParticipantGroup( studyId: UUID, groupId: UUID ) =
        invoke( RecruitmentServiceRequest.StopParticipantGroup( studyId, groupId ) )
}


object RecruitmentServiceInvoker : ApplicationServiceInvoker<RecruitmentService, RecruitmentServiceRequest<*>>
{
    override suspend fun RecruitmentServiceRequest<*>.invoke( service: RecruitmentService ): Any =
        when ( this )
        {
            is RecruitmentServiceRequest.AddParticipant -> service.addParticipant( studyId, email )
            is RecruitmentServiceRequest.GetParticipant -> service.getParticipant( studyId, participantId )
            is RecruitmentServiceRequest.GetParticipants -> service.getParticipants( studyId )
            is RecruitmentServiceRequest.InviteNewParticipantGroup ->
                service.inviteNewParticipantGroup( studyId, group )
            is RecruitmentServiceRequest.GetParticipantGroupStatusList ->
                service.getParticipantGroupStatusList( studyId )
            is RecruitmentServiceRequest.StopParticipantGroup -> service.stopParticipantGroup( studyId, groupId )
        }
}