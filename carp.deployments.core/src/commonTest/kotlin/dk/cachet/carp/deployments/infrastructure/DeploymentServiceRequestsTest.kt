package dk.cachet.carp.deployments.infrastructure

import dk.cachet.carp.common.application.UUID
import dk.cachet.carp.common.application.devices.DefaultDeviceRegistration
import dk.cachet.carp.common.infrastructure.services.ApplicationServiceLog
import dk.cachet.carp.common.test.infrastructure.ApplicationServiceRequestsTest
import dk.cachet.carp.deployments.application.DeploymentService
import dk.cachet.carp.deployments.application.DeploymentServiceHostTest
import dk.cachet.carp.protocols.infrastructure.test.createEmptyProtocol
import kotlinx.datetime.Clock


/**
 * Tests for [DeploymentServiceRequest]'s.
 */
class DeploymentServiceRequestsTest : ApplicationServiceRequestsTest<DeploymentService, DeploymentServiceRequest<*>>(
    DeploymentServiceRequest.Serializer,
    REQUESTS
)
{
    companion object
    {
        private val REQUESTS: List<DeploymentServiceRequest<*>> = listOf(
            DeploymentServiceRequest.CreateStudyDeployment( UUID.randomUUID(), createEmptyProtocol().getSnapshot(), listOf() ),
            DeploymentServiceRequest.RemoveStudyDeployments( emptySet() ),
            DeploymentServiceRequest.GetStudyDeploymentStatus( UUID.randomUUID() ),
            DeploymentServiceRequest.GetStudyDeploymentStatusList( setOf( UUID.randomUUID() ) ),
            DeploymentServiceRequest.RegisterDevice( UUID.randomUUID(), "Test role", DefaultDeviceRegistration() ),
            DeploymentServiceRequest.UnregisterDevice( UUID.randomUUID(), "Test role" ),
            DeploymentServiceRequest.GetDeviceDeploymentFor( UUID.randomUUID(), "Test role" ),
            DeploymentServiceRequest.DeviceDeployed( UUID.randomUUID(), "Test role", Clock.System.now() ),
            DeploymentServiceRequest.Stop( UUID.randomUUID() )
        )
    }


    override fun createServiceLog(): ApplicationServiceLog<DeploymentService, DeploymentService.Event> =
        DeploymentServiceHostTest
            .createService()
            .let { DeploymentServiceLog( it.first, it.second ) }
}
