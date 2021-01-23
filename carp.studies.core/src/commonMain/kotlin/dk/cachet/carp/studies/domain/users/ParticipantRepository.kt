package dk.cachet.carp.studies.domain.users

import dk.cachet.carp.common.UUID
import dk.cachet.carp.studies.domain.Recruitment


/**
 * Store [Participant] instances, linked to study IDs.
 */
interface ParticipantRepository
{
    /**
     * Adds a new [participant] for the study with [studyId] to the repository.
     *
     * @throws IllegalArgumentException when a participant with the specified ID already exists within the study.
     */
    suspend fun addParticipant( studyId: UUID, participant: Participant )

    /**
     * Returns the participants which were added to the study with the specified [studyId].
     */
    suspend fun getParticipants( studyId: UUID ): List<Participant>

    /**
     * Add a new [Recruitment] to the repository.
     *
     * @throws IllegalArgumentException when a recruitment with the same studyId already exists.
     */
    suspend fun addRecruitment( recruitment: Recruitment )

    /**
     * Returns the [Recruitment] for the specified [studyId], or null when no recruitment is found.
     */
    suspend fun getRecruitment( studyId: UUID ): Recruitment?

    /**
     * Update a [Recruitment] which is already stored in this repository.
     *
     * @throws IllegalArgumentException when no previous version of this recruitment is stored in the repository.
     */
    suspend fun updateRecruitment( recruitment: Recruitment )

    /**
     * Remove all data (participants and recruitment) for the study with [studyId].
     *
     * @return True when all data for the study was removed; false when no data for the study is present in the repository.
     */
    suspend fun removeStudy( studyId: UUID ): Boolean
}
