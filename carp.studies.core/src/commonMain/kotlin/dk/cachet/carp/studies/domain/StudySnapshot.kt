package dk.cachet.carp.studies.domain

import dk.cachet.carp.common.DateTime
import dk.cachet.carp.common.UUID
import dk.cachet.carp.common.ddd.Snapshot
import dk.cachet.carp.deployment.domain.users.StudyInvitation
import dk.cachet.carp.protocols.domain.StudyProtocolSnapshot
import dk.cachet.carp.studies.domain.users.DeanonymizedParticipation
import kotlinx.serialization.Serializable


@Serializable
data class StudySnapshot(
    val studyId: UUID,
    val ownerId: UUID,
    val name: String,
    val description: String,
    val invitation: StudyInvitation,
    override val creationDate: DateTime,
    val protocolSnapshot: StudyProtocolSnapshot?,
    val isLive: Boolean,
    val participations: Map<UUID, Set<DeanonymizedParticipation>>
) : Snapshot<Study>()
{
    companion object
    {
        /**
         * Create a snapshot of the specified [Study].
         *
         * @param study The [Study] to create a snapshot for.
         */
        fun fromStudy( study: Study ): StudySnapshot
        {
            val clonedParticipations: MutableMap<UUID, Set<DeanonymizedParticipation>> = mutableMapOf()
            for ( p in study.participations )
            {
                clonedParticipations[ p.key ] = p.value.toSet()
            }

            return StudySnapshot(
                studyId = study.id,
                ownerId = study.owner.id,
                name = study.name,
                description = study.description,
                invitation = study.invitation,
                creationDate = study.creationDate,
                protocolSnapshot = study.protocolSnapshot,
                isLive = study.isLive,
                participations = clonedParticipations )
        }
    }

    override fun toObject(): Study = Study.fromSnapshot( this )
}
