package dk.cachet.carp.clients.domain

import dk.cachet.carp.clients.domain.study.Study
import dk.cachet.carp.common.application.UUID
import dk.cachet.carp.common.infrastructure.test.StubPrimaryDeviceConfiguration
import dk.cachet.carp.deployments.application.DeviceDeploymentStatus
import dk.cachet.carp.deployments.application.PrimaryDeviceDeployment
import dk.cachet.carp.deployments.application.StudyDeploymentStatus
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*


interface ClientRepositoryTest
{
    /**
     * Called for each test to create a repository to run tests on.
     */
    fun createRepository(): ClientRepository


    @Test
    fun deviceRegistration_is_initially_null() = runTest {
        val repo = createRepository()
        assertNull( repo.getDeviceRegistration() )
    }

    @Test
    fun addStudy_can_be_retrieved() = runTest {
        val repo = createRepository()

        val study = Study( UUID.randomUUID(), "Device role" )
        repo.addStudy( study )

        // Study can be retrieved by ID.
        val retrievedById = repo.getStudy( study.id )
        assertNotNull( retrievedById )

        // Study can be retrieved by deployment.
        val deploymentId = study.studyDeploymentId
        val roleName = study.deviceRoleName
        val retrievedStudy = repo.getStudyByDeployment( deploymentId, roleName )
        assertNotNull( retrievedStudy )

        // Study is included in list.
        val allStudies = repo.getStudyList()
        assertEquals( 1, allStudies.count() )
        assertNotNull( allStudies.single { it.studyDeploymentId == deploymentId && it.deviceRoleName == roleName } )
    }

    @Test
    fun addStudy_fails_for_existing_study() = runTest {
        val repo = createRepository()
        val study = Study( UUID.randomUUID(), "Device role" )
        repo.addStudy( study )

        assertFailsWith<IllegalArgumentException> { repo.addStudy( study ) }
    }

    @Test
    fun getStudy_is_null_for_unknown_study() = runTest {
        val repo = createRepository()

        val unknownId = UUID.randomUUID()
        assertNull( repo.getStudy( unknownId ) )
    }

    @Test
    fun getStudyByDeployment_is_null_for_unknown_study() = runTest {
        val repo = createRepository()

        val unknownId = UUID.randomUUID()
        assertNull( repo.getStudyByDeployment( unknownId, "Unknown" ) )
    }

    @Test
    fun getStudyList_is_empty_initially() = runTest {
        val repo = createRepository()

        assertEquals( 0, repo.getStudyList().count() )
    }

    @Test
    fun updateStudy_succeeds() = runTest {
        val repo = createRepository()
        val deploymentId = UUID.randomUUID()
        val deviceRoleName = "Device role"
        val study = Study( deploymentId, deviceRoleName )
        repo.addStudy( study )

        // Make some changes and update.
        val primaryDevice = StubPrimaryDeviceConfiguration( deviceRoleName )
        val registration = primaryDevice.createRegistration()
        val primaryDeviceDeployment = PrimaryDeviceDeployment( primaryDevice, registration )
        study.deploymentStatusReceived(
            StudyDeploymentStatus.DeployingDevices(
                Clock.System.now(),
                deploymentId,
                listOf(
                    DeviceDeploymentStatus.Registered( primaryDevice, registration, true, emptySet(), emptySet() )
                ),
                emptyList(),
                null
            )
        )
        study.deviceDeploymentReceived( primaryDeviceDeployment )
        repo.updateStudy( study )

        // Verify whether changes were stored.
        val retrievedStudy = repo.getStudyByDeployment( deploymentId, deviceRoleName )
        assertNotNull( retrievedStudy )
        assertEquals( study.getSnapshot(), retrievedStudy.getSnapshot() )
    }

    @Test
    fun updateStudy_fails_for_unknown_study() = runTest {
        val repo = createRepository()

        val study = Study( UUID.randomUUID(), "Device role" )
        assertFailsWith<IllegalArgumentException> { repo.updateStudy( study ) }
    }

    @Test
    fun removeStudy_succeeds() = runTest {
        val repo = createRepository()
        val study = Study( UUID.randomUUID(), "Device role" )
        repo.addStudy( study )

        repo.removeStudy( study )

        val deploymentId = study.studyDeploymentId
        val roleName = study.deviceRoleName
        assertNull( repo.getStudyByDeployment( deploymentId, roleName ) )
        assertEquals( 0, repo.getStudyList().count() )
    }

    @Test
    fun removeStudy_succeeds_when_study_not_present() = runTest {
        val repo = createRepository()
        val study = Study( UUID.randomUUID(), "Device role" )
        repo.removeStudy( study )
    }
}
