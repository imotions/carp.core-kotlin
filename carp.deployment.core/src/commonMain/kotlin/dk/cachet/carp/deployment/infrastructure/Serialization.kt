package dk.cachet.carp.deployment.infrastructure

import dk.cachet.carp.common.serialization.createDefaultJSON
import dk.cachet.carp.deployment.domain.*
import dk.cachet.carp.protocols.infrastructure.PROTOCOLS_SERIAL_MODULE
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.*


/**
 * Create a [Json] serializer adopting a default CARP infrastructure configuration with all [dk.cachet.carp.deployment] types registered.
 * This ensures a global configuration on how serialization should occur.
 * Additional types the serializer needs to be aware about (such as polymorph extending classes) should be registered through [module].
 */
fun createDeploymentSerializer( module: SerialModule = EmptyModule ): Json
{
    return createDefaultJSON( PROTOCOLS_SERIAL_MODULE + module )
}

/**
 * A default CARP infrastructure serializer capable of serializing all [dk.cachet.carp.deployment] types.
 * In case custom extending types are defined, this variable should be reassigned for serialization extension functions to work as expected.
 * [createDeploymentSerializer] can be used to this end, by including all extending types in the [SerialModule] as parameter.
 */
var JSON: Json = createDeploymentSerializer()

/**
 * Create a [StudyDeploymentSnapshot] from JSON, serialized using the globally set infrastructure serializer ([JSON]).
 */
fun StudyDeploymentSnapshot.Companion.fromJson( json: String ): StudyDeploymentSnapshot
    = JSON.parse( serializer(), json )

/**
 * Serialize to JSON, using the globally set infrastructure serializer ([JSON]).
 */
fun StudyDeploymentSnapshot.toJson(): String
    = JSON.stringify( StudyDeploymentSnapshot.serializer(), this )

/**
 * Create a [StudyDeploymentStatus] from JSON, serialized using the globally set infrastructure serializer ([JSON]).
 */
fun StudyDeploymentStatus.Companion.fromJson( json: String ): StudyDeploymentStatus
    = JSON.parse( serializer(), json )

/**
 * Serialize to JSON, using the globally set infrastructure serializer ([JSON]).
 */
fun StudyDeploymentStatus.toJson(): String
    = JSON.stringify( StudyDeploymentStatus.serializer(), this )

/**
 * Create a [MasterDeviceDeployment] from JSON, serialized using the globally set infrastructure serializer ([JSON]).
 */
fun MasterDeviceDeployment.Companion.fromJson( json: String ): MasterDeviceDeployment
    = JSON.parse( serializer(), json )

/**
 * Serialize to JSON, using the globally set infrastructure serializer ([JSON]).
 */
fun MasterDeviceDeployment.toJson(): String
    = JSON.stringify( MasterDeviceDeployment.serializer(), this )