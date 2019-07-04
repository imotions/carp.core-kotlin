package dk.cachet.carp.protocols.domain.devices

import dk.cachet.carp.common.Trilean
import dk.cachet.carp.common.serialization.UnknownPolymorphicWrapper
import kotlinx.serialization.json.*


/**
 * A wrapper used to load extending types from [MasterDeviceDescriptor] serialized as JSON which are unknown at runtime.
 */
data class CustomMasterDeviceDescriptor( override val className: String, override val jsonSource: String )
    : MasterDeviceDescriptor(), UnknownPolymorphicWrapper
{
    override val roleName: String

    init
    {
        val json = Json.plain.parseJson( jsonSource ) as JsonObject

        val roleNameField = MasterDeviceDescriptor::roleName.name
        if ( !json.containsKey( roleNameField ) )
        {
            throw IllegalArgumentException( "No '$roleNameField' defined." )
        }
        roleName = json[ roleNameField ]!!.content
    }

    override fun createRegistration(): DeviceRegistration
        = throw UnsupportedOperationException( "The concrete type of this device is not known. Therefore, it is unknown which registration is required." )

    /**
     * For unknown types, it cannot be determined whether or not a given configuration is valid.
     */
    override fun isValidConfiguration( registration: DeviceRegistration ) = Trilean.UNKNOWN
}