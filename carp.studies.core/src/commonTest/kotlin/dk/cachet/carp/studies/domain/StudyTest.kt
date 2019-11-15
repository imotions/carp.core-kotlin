package dk.cachet.carp.studies.domain

import dk.cachet.carp.common.UUID
import kotlin.test.*


/**
 * Tests for [Study].
 */
class StudyTest
{
    private fun createTestStudy(): Study
    {
        val owner = StudyOwner()
        val id = UUID.randomUUID()
        return Study( owner, "Test study", StudyDescription.empty(), id )
    }

    @Test
    fun includeParticipant_succeeds()
    {
        val study: Study = createTestStudy()
        val participantId = UUID.randomUUID()

        study.includeParticipant( participantId )

        assertEquals( participantId, study.participantIds.single() )
    }

    @Test
    fun includeParticipant_multiple_times_only_adds_once()
    {
        val study: Study = createTestStudy()
        val participantId = UUID.randomUUID()

        study.includeParticipant( participantId )
        study.includeParticipant( participantId )

        assertEquals( participantId, study.participantIds.single() )
    }

    @Test
    fun creating_study_fromSnapshot_obtained_by_getSnapshot_is_the_same()
    {
        val owner = StudyOwner()
        val description = StudyDescription( "Test" )
        val studyId = UUID.randomUUID()
        val participantId = UUID.randomUUID()
        val study = Study( owner, "Test study", description, studyId )
        study.includeParticipant( participantId )

        val snapshot = study.getSnapshot()
        val fromSnapshot = Study.fromSnapshot( snapshot )

        assertEquals( studyId, fromSnapshot.id )
        assertEquals( owner, fromSnapshot.owner )
        assertEquals( "Test study", fromSnapshot.name )
        assertEquals( description, fromSnapshot.description )
        assertEquals( participantId, fromSnapshot.participantIds.single() )
    }
}