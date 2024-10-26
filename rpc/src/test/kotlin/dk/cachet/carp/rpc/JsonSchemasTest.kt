package dk.cachet.carp.rpc

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaException
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import java.net.URI
import kotlin.test.*


class JsonSchemasTest
{
    @Test
    fun schema_validation_for_generated_examples_succeeds()
    {
        val schemaFactory = JsonSchemaFactory.getInstance( SpecVersion.VersionFlag.V201909 )
        val mapper = ObjectMapper()

        for ( (appService, requests) in exampleApplicationServiceRequests )
        {
            // Find request schema, or skip in case no schema is defined yet.
            val schemaPath = appService.requestSchemaUri.path.replaceFirst( "/", "" )
            val requestSchemaURI = URI( "file:$schemaPath" )
            val requestSchema =
                try { schemaFactory.getSchema( requestSchemaURI ) }
                catch ( _: JsonSchemaException ) { null }
            checkNotNull( requestSchema )
                { "Could not locate JSON schema for \"${appService.requestObjectName}\". Searched for: $requestSchemaURI" }

            @Suppress( "UnreachableCode" ) // Seemingly a bug in detekt 1.20.0-RC2
            for ( r in requests )
            {
                // Validate request.
                val requestJson = mapper.readTree( r.requestObject.json )
                val requestErrors = requestSchema.validate( requestJson )
                check( requestErrors.isEmpty() )
                {
                    "JSON schema \"${requestSchema.schemaLocation}\" " +
                    "doesn't match generated JSON example of \"${r.requestObject.klass}\": $requestErrors"
                }

                // Validate response.
                val requestObjectName = r.requestObject.klass.simpleName
                val responseSchemaNode = requestSchema.getRefSchemaNode( "#/\$defs/$requestObjectName/Response" )
                val responseSchema = schemaFactory.getSchema( requestSchema.schemaLocation, responseSchemaNode )
                val responseJson = mapper.readTree( r.response.json )
                val responseErrors = responseSchema.validate( responseJson )
                check( responseErrors.isEmpty() )
                {
                    "JSON schema response defined in \"${requestSchema.schemaLocation}\" for \"$requestObjectName\" " +
                    "doesn't match generated JSON example of \"${r.response.klass.simpleName}\": $responseErrors"
                }
            }
        }
    }
}
