package dk.cachet.carp.protocols.infrastructure

import dk.cachet.carp.common.application.UUID
import dk.cachet.carp.common.infrastructure.services.ApplicationServiceDecorator
import dk.cachet.carp.common.infrastructure.services.Command
import dk.cachet.carp.protocols.application.ProtocolFactoryService
import dk.cachet.carp.protocols.application.StudyProtocolSnapshot


class ProtocolFactoryServiceDecorator(
    service: ProtocolFactoryService,
    requestDecorator: (Command<ProtocolFactoryServiceRequest<*>>) -> Command<ProtocolFactoryServiceRequest<*>>
) : ApplicationServiceDecorator<ProtocolFactoryService, ProtocolFactoryServiceRequest<*>>( service, requestDecorator ),
    ProtocolFactoryService
{
    override suspend fun createCustomProtocol(
        ownerId: UUID,
        name: String,
        customProtocol: String,
        description: String?
    ): StudyProtocolSnapshot = invoke(
        ProtocolFactoryServiceRequest.CreateCustomProtocol( ownerId, name, customProtocol, description )
    )

    override suspend fun ProtocolFactoryServiceRequest<*>.invokeOnService( service: ProtocolFactoryService ): Any =
        when ( this )
        {
            is ProtocolFactoryServiceRequest.CreateCustomProtocol ->
                service.createCustomProtocol( ownerId, name, customProtocol, description )
        }
}
