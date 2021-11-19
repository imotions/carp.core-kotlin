package dk.cachet.carp.common.infrastructure.test

import dk.cachet.carp.common.application.Trilean
import dk.cachet.carp.common.application.data.DataType
import dk.cachet.carp.common.application.devices.DefaultDeviceRegistration
import dk.cachet.carp.common.application.devices.DefaultDeviceRegistrationBuilder
import dk.cachet.carp.common.application.devices.DeviceDescriptor
import dk.cachet.carp.common.application.sampling.DataTypeSamplingSchemeMap
import dk.cachet.carp.common.application.sampling.SamplingConfiguration
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass


/**
 * A stub [DeviceDescriptor] which can measure [STUB_DATA_TYPE].
 */
@Serializable
data class StubDeviceDescriptor(
    @Required override val roleName: String = "Stub device",
    override val isOptional: Boolean = false,
    override val defaultSamplingConfiguration: Map<DataType, SamplingConfiguration> = emptyMap()
) :
    DeviceDescriptor<DefaultDeviceRegistration, DefaultDeviceRegistrationBuilder>()
{
    object Sensors : DataTypeSamplingSchemeMap()
    {
        val STUB_DATA = add( StubDataTypeSamplingScheme() )
    }

    override fun getSupportedDataTypes(): Set<DataType> = Sensors.keys
    override fun getDataTypeSamplingSchemes(): DataTypeSamplingSchemeMap = Sensors
    override fun createDeviceRegistrationBuilder(): DefaultDeviceRegistrationBuilder = DefaultDeviceRegistrationBuilder()
    override fun getRegistrationClass(): KClass<DefaultDeviceRegistration> = DefaultDeviceRegistration::class
    override fun isValidRegistration( registration: DefaultDeviceRegistration ): Trilean = Trilean.TRUE
}
