package dk.cachet.carp.studies.domain

import dk.cachet.carp.common.application.UUID
import dk.cachet.carp.common.domain.Snapshot
import dk.cachet.carp.deployments.application.users.StudyInvitation
import dk.cachet.carp.protocols.application.StudyProtocolSnapshot
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable


@Serializable
data class StudySnapshot(
    override val id: UUID,
    override val createdOn: Instant,
    val ownerId: UUID,
    val name: String,
    val description: String? = null,
    val invitation: StudyInvitation,
    val protocolSnapshot: StudyProtocolSnapshot?,
    val isLive: Boolean
) : Snapshot<Study>
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
            return StudySnapshot(
                id = study.id,
                createdOn = study.createdOn,
                ownerId = study.ownerId,
                name = study.name,
                description = study.description,
                invitation = study.invitation,
                protocolSnapshot = study.protocolSnapshot,
                isLive = study.isLive )
        }
    }

    override fun toObject(): Study = Study.fromSnapshot( this )
}
