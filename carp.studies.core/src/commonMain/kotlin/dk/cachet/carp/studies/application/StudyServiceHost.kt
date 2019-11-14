package dk.cachet.carp.studies.application

import dk.cachet.carp.common.UUID
import dk.cachet.carp.studies.domain.*


/**
 * Implementation of [StudyService] which allows creating and managing studies.
 */
class StudyServiceHost( private val repository: StudyRepository ) : StudyService
{
    /**
     * Create a new study for the specified [owner].
     *
     * @param name A descriptive name for the study, assigned by, and only visible to, the [owner].
     * @param description A description of the study, visible to all participants.
     */
    override suspend fun createStudy( owner: StudyOwner, name: String, description: StudyDescription? ): StudyStatus
    {
        val ensuredDescription = description ?: StudyDescription( name )
        val study = Study( owner, name, ensuredDescription )

        repository.add( study )

        return study.getStatus()
    }

    /**
     * Get the status for a study with the given [studyId].
     *
     * @param studyId The id of the study to return [StudyStatus] for.
     *
     * @throws IllegalArgumentException when a deployment with [studyId] does not exist.
     */
    override suspend fun getStudyStatus( studyId: UUID ): StudyStatus
    {
        val study = repository.getById( studyId )
        require( study != null )

        return study.getStatus()
    }
}