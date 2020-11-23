package dk.cachet.carp.deployment.infrastructure

import dk.cachet.carp.common.UUID
import dk.cachet.carp.common.ddd.ServiceInvoker
import dk.cachet.carp.common.ddd.createServiceInvoker
import dk.cachet.carp.common.users.AccountIdentity
import dk.cachet.carp.deployment.application.ParticipationService
import dk.cachet.carp.deployment.domain.users.ActiveParticipationInvitation
import dk.cachet.carp.deployment.domain.users.Participation
import dk.cachet.carp.deployment.domain.users.StudyInvitation
import kotlinx.serialization.Serializable

// TODO: Due to a bug, `Service` and `Invoker` cannot be used here, although that would be preferred.
//       Change this once this is fixed: https://youtrack.jetbrains.com/issue/KT-24700
private typealias ParticipationServiceInvoker<T> = ServiceInvoker<ParticipationService, T>
// private typealias Service = ParticipationService
// private typealias Invoker<T> = ServiceInvoker<ParticipationService, T>


/**
 * Serializable application service requests to [ParticipationService] which can be executed on demand.
 */
@Serializable
sealed class ParticipationServiceRequest
{
    @Serializable
    data class AddParticipation( val studyDeploymentId: UUID, val deviceRoleNames: Set<String>, val identity: AccountIdentity, val invitation: StudyInvitation ) :
        ParticipationServiceRequest(),
        ParticipationServiceInvoker<Participation> by createServiceInvoker( ParticipationService::addParticipation, studyDeploymentId, deviceRoleNames, identity, invitation )

    @Serializable
    data class GetActiveParticipationInvitations( val accountId: UUID ) :
        ParticipationServiceRequest(),
        ParticipationServiceInvoker<Set<ActiveParticipationInvitation>> by createServiceInvoker( ParticipationService::getActiveParticipationInvitations, accountId )
}