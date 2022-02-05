package dk.cachet.carp.protocols.infrastructure.versioning

import dk.cachet.carp.common.test.infrastructure.versioning.OutputTestRequests
import dk.cachet.carp.protocols.application.ProtocolService
import dk.cachet.carp.protocols.application.ProtocolServiceHostTest
import dk.cachet.carp.protocols.application.ProtocolServiceTest
import dk.cachet.carp.protocols.infrastructure.ProtocolServiceLog


class OutputProtocolServiceTestRequests :
    OutputTestRequests<ProtocolService>(
        ProtocolService::class,
        ProtocolServiceLog( ProtocolServiceHostTest.createService() )
    ),
    ProtocolServiceTest
{
    override fun createService(): ProtocolService = loggedService
}