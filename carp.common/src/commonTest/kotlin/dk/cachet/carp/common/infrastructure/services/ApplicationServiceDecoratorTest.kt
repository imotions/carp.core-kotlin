package dk.cachet.carp.common.infrastructure.services

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals


private typealias TestService = ApplicationServiceRequestTest.TestService
private typealias TestServiceRequest = ApplicationServiceRequestTest.TestServiceRequest<*>

class ApplicationServiceDecoratorTest
{
    class TestServiceDecorator(
        service: TestService,
        requestDecorator: (Command<TestServiceRequest>) -> Command<TestServiceRequest>
    ) : ApplicationServiceDecorator<TestService, TestServiceRequest>( service, requestDecorator ),
        TestService
    {
        override suspend fun operation( parameter: Int ): Int = invoke(
            ApplicationServiceRequestTest.TestServiceRequest.Operation( parameter )
        )

        override suspend fun TestServiceRequest.invokeOnService( service: TestService ): Any? =
            when ( this )
            {
                is ApplicationServiceRequestTest.TestServiceRequest.Operation -> service.operation( parameter )
            }
    }


    @Test
    fun can_add_multiple_decorators() = runTest {
        val invokedDecorators = mutableListOf<String>()

        class Decorator<TRequest>( val name: String, val decoratee: Command<TRequest> ) : Command<TRequest>
        {
            override suspend fun <TReturn> invoke( request: TRequest ): TReturn
            {
                invokedDecorators.add( name )
                return decoratee.invoke( request )
            }
        }


        val host =
            object : TestService
            {
                override suspend fun operation( parameter: Int ): Int = parameter
            }
        val decorator = TestServiceDecorator( host )
            {
                Decorator(
                    "first",
                    Decorator( "second", it ),
                )
            }

        val result = decorator.operation( 42 )
        assertEquals( 42, result )
        assertEquals( listOf( "first", "second" ), invokedDecorators )
    }
}
