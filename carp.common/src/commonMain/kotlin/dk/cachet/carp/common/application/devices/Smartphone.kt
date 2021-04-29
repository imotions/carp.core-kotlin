package dk.cachet.carp.common.application.devices

import dk.cachet.carp.common.application.TimeSpan
import dk.cachet.carp.common.application.Trilean
import dk.cachet.carp.common.application.data.CarpDataTypes
import dk.cachet.carp.common.application.data.DataType
import dk.cachet.carp.common.application.sampling.DataTypeSamplingSchemeList
import dk.cachet.carp.common.application.sampling.IntervalSamplingConfigurationBuilder
import dk.cachet.carp.common.application.sampling.IntervalSamplingScheme
import dk.cachet.carp.common.application.sampling.NoOptionsSamplingScheme
import dk.cachet.carp.common.application.sampling.SamplingConfiguration
import dk.cachet.carp.common.application.sampling.SamplingConfigurationMapBuilder
import dk.cachet.carp.common.application.tasks.TaskDescriptorList
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass


typealias SmartphoneDeviceRegistration = DefaultDeviceRegistration
typealias SmartphoneDeviceRegistrationBuilder = DefaultDeviceRegistrationBuilder


/**
 * An internet-connected phone with built-in sensors.
 */
@Serializable
data class Smartphone(
    override val roleName: String,
    override val defaultSamplingConfiguration: Map<DataType, SamplingConfiguration>
) : MasterDeviceDescriptor<SmartphoneDeviceRegistration, SmartphoneDeviceRegistrationBuilder>()
{
    constructor( roleName: String, builder: SmartphoneBuilder.() -> Unit = { } ) :
        this( roleName, SmartphoneBuilder().apply( builder ).buildSamplingConfiguration() )

    /**
     * All the sensors commonly available on smartphones.
     */
    object Sensors : DataTypeSamplingSchemeList()
    {
        /**
         *  Geographic location data, representing latitude and longitude within the World Geodetic System 1984.
         */
        val GEOLOCATION = add(
            IntervalSamplingScheme( CarpDataTypes.GEOLOCATION, TimeSpan.fromMinutes( 1.0 ) )
        )

        /**
         * Steps within recorded time intervals as reported by a phone's dedicated hardware sensor.
         * Data rate is determined by the sensor.
         *
         * Android (https://developer.android.com/guide/topics/sensors/sensors_motion#sensors-motion-stepcounter):
         * - There is a latency of up to 10 s.
         * - Only available starting from Android 4.4.
         *
         * TODO: Android can also 'listen' for steps, which has a delay of about 2 s but is less accurate.
         *       Each 'step' is reported as an event, so this would map to a different DataType (e.g. `Step`).
         *       Not certain this is available on iPhone.
         */
        val STEP_COUNT = add( NoOptionsSamplingScheme( CarpDataTypes.STEP_COUNT ) ) // No configuration options available.
    }

    /**
     * ALl tasks commonly available on smartphones.
     */
    object Tasks : TaskDescriptorList()

    override fun getSupportedDataTypes(): Set<DataType> = Sensors.map { it.type }.toSet()

    override fun createDeviceRegistrationBuilder(): SmartphoneDeviceRegistrationBuilder = SmartphoneDeviceRegistrationBuilder()
    override fun getRegistrationClass(): KClass<SmartphoneDeviceRegistration> = SmartphoneDeviceRegistration::class
    override fun isValidConfiguration( registration: SmartphoneDeviceRegistration ) = Trilean.TRUE
}


/**
 * A helper class to configure and construct immutable [Smartphone] classes.
 */
class SmartphoneBuilder : DeviceDescriptorBuilder<SmartphoneSamplingConfigurationMapBuilder>()
{
    override fun createSamplingConfigurationMapBuilder(): SmartphoneSamplingConfigurationMapBuilder =
        SmartphoneSamplingConfigurationMapBuilder()
}


/**
 * A helper class to construct sampling configurations for a [Smartphone].
 */
class SmartphoneSamplingConfigurationMapBuilder : SamplingConfigurationMapBuilder()
{
    /**
     * Configure sampling configuration for [CarpDataTypes.GEOLOCATION].
     */
    fun geolocation( builder: IntervalSamplingConfigurationBuilder.() -> Unit ): SamplingConfiguration =
        addConfiguration( Smartphone.Sensors.GEOLOCATION, builder )
}