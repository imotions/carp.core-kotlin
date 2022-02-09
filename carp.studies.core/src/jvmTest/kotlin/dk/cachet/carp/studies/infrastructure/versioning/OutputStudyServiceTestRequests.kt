package dk.cachet.carp.studies.infrastructure.versioning

import dk.cachet.carp.common.test.infrastructure.versioning.OutputTestRequests
import dk.cachet.carp.studies.application.StudyService
import dk.cachet.carp.studies.application.StudyServiceHostTest
import dk.cachet.carp.studies.application.StudyServiceTest
import dk.cachet.carp.studies.infrastructure.StudyServiceLog


private val services = StudyServiceHostTest.createService()

class OutputStudyServiceTestRequests :
    OutputTestRequests<StudyService>(
        StudyService::class,
        StudyServiceLog( services.first, services.second )
    ),
    StudyServiceTest
{
    override fun createService(): StudyService = loggedService
}
