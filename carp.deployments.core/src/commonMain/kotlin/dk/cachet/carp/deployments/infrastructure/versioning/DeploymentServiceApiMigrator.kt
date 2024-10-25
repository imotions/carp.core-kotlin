package dk.cachet.carp.deployments.infrastructure.versioning

import dk.cachet.carp.common.application.services.ApiVersion
import dk.cachet.carp.common.infrastructure.versioning.*
import dk.cachet.carp.deployments.application.DeploymentService
import dk.cachet.carp.deployments.infrastructure.DeploymentServiceInvoker
import dk.cachet.carp.deployments.infrastructure.DeploymentServiceRequest
import kotlinx.serialization.json.*


private val major1Minor0To1Migration =
    object : Major1Minor0To1Migration()
    {
        override fun migrateRequest( request: JsonObject ) = request.migrate {
            ifType( "dk.cachet.carp.deployments.infrastructure.DeploymentServiceRequest.CreateStudyDeployment" ) {
                addVersionField( "protocol" )
            }
        }

        override fun migrateResponse( request: JsonObject, response: ApiResponse, targetVersion: ApiVersion ) = response

        override fun migrateEvent( event: JsonObject ) = event.migrate {
            ifType( "dk.cachet.carp.deployments.application.DeploymentService.Event.StudyDeploymentCreated" ) {
                addVersionField( "protocol" )
            }
        }
    }


private val major1Minor1To3Migration =
    @Suppress( "MagicNumber" )
    object : ApiMigration( 1, 3 )
    {
        override fun migrateRequest( request: JsonObject ) = request

        override fun migrateResponse( request: JsonObject, response: ApiResponse, targetVersion: ApiVersion ) =
            when ( request.getType( ) )
            {
                "dk.cachet.carp.deployments.infrastructure.DeploymentServiceRequest.GetDeviceDeploymentFor" ->
                {
                    val responseObject = response.response?.jsonObject?.migrate {
                        requiredDeviceDisplayName( "registration" )

                        updateOptionalObject( "connectedDeviceRegistrations" )
                        {
                            for ( connectedDevice in json.keys ) requiredDeviceDisplayName( connectedDevice )
                        }
                    }
                    ApiResponse( responseObject, response.ex )
                }
                "dk.cachet.carp.deployments.infrastructure.DeploymentServiceRequest.CreateStudyDeployment",
                "dk.cachet.carp.deployments.infrastructure.DeploymentServiceRequest.GetStudyDeploymentStatus",
                "dk.cachet.carp.deployments.infrastructure.DeploymentServiceRequest.RegisterDevice",
                "dk.cachet.carp.deployments.infrastructure.DeploymentServiceRequest.UnregisterDevice",
                "dk.cachet.carp.deployments.infrastructure.DeploymentServiceRequest.DeviceDeployed",
                "dk.cachet.carp.deployments.infrastructure.DeploymentServiceRequest.Stop" ->
                {
                    val responseObject = response.response?.jsonObject?.migrate {
                        removeDeviceRegistration()
                    }
                    ApiResponse( responseObject, response.ex )
                }
                "dk.cachet.carp.deployments.infrastructure.DeploymentServiceRequest.GetStudyDeploymentStatusList" ->
                {
                    val responseObject = response.response?.jsonArray?.migrate {
                        objects { removeDeviceRegistration() }
                    }
                    ApiResponse( responseObject, response.ex )
                }
                else -> response
            }

        /**
         * The `deviceDisplayName` field in `DeviceRegistration`, with default value `null`, was changed from required
         * into optional. So, if it isn't set, it needs to bet set to `null` explicitly.
         */
        private fun ApiJsonObjectMigrationBuilder.requiredDeviceDisplayName( fieldName: String ) =
            updateObject( fieldName ) {
                json.getOrPut( "deviceDisplayName" ) { JsonNull }
            }

        /**
         * Remove the newly added `deviceRegistration` field from `DeviceDeploymentStatus` objects contained within
         * `StudyDeploymentStatus`.
         */
        private fun ApiJsonObjectMigrationBuilder.removeDeviceRegistration() =
            updateArray( "deviceStatusList" ) {
                objects { json.remove( "deviceRegistration" ) }
            }

        override fun migrateEvent( event: JsonObject ) = event
    }


val DeploymentServiceApiMigrator = ApplicationServiceApiMigrator(
    DeploymentService.API_VERSION,
    DeploymentServiceInvoker,
    DeploymentServiceRequest.Serializer,
    DeploymentService.Event.serializer(),
    listOf( major1Minor0To1Migration, major1Minor1To3Migration )
)
