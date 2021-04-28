package dk.cachet.carp.common.application.sampling

import dk.cachet.carp.common.application.EnumObjectList
import dk.cachet.carp.common.application.data.DataType
import dk.cachet.carp.common.application.tasks.Measure


/**
 * Specifies the sampling scheme for a [DataType], including possible options, defaults, and constraints.
 */
abstract class DataTypeSamplingScheme<TSamplingConfigurationBuilder : SamplingConfigurationBuilder<*>>(
    /**
     * The [DataType] this sampling scheme relates to.
     */
    val type: DataType
)
{
    /**
     * Create a [SamplingConfigurationBuilder] to help construct a matching [SamplingConfiguration] for [type].
     */
    protected abstract fun createSamplingConfigurationBuilder(): TSamplingConfigurationBuilder

    /**
     * Create a [SamplingConfiguration] which can be used to configure measures of [type].
     *
     * @throws IllegalArgumentException when a sampling configuration is built which breaks constraints specified in this sampling scheme.
     */
    fun samplingConfiguration( builder: TSamplingConfigurationBuilder.() -> Unit ): SamplingConfiguration =
        createSamplingConfigurationBuilder().apply( builder ).build( this )

    /**
     * Create a [Measure] for the [type] defined by this sampling scheme,
     * and optionally override the device's default [SamplingConfiguration] for this [type].
     *
     * @throws IllegalArgumentException when a sampling configuration is built which breaks constraints specified in this sampling scheme.
     */
    fun measure( samplingConfigurationBuilder: (TSamplingConfigurationBuilder.() -> Unit)? = null ): Measure =
        Measure( type, samplingConfigurationBuilder?.let { samplingConfiguration( it ) } )

    /**
     * Determines whether [configuration] is valid for the constraints defined in this sampling scheme.
     */
    abstract fun isValid( configuration: SamplingConfiguration ): Boolean
}


/**
 * A helper class to construct iterable objects which hold [DataTypeSamplingScheme] member definitions.
 * This is similar to an enum, but removes the need for an intermediate enum type and generic type parameters are retained per member.
 *
 * Extend from this class as an object and assign members as follows: `val SCHEME = add( Scheme( options ) )`.
 */
abstract class DataTypeSamplingSchemeList : EnumObjectList<DataTypeSamplingScheme<*>>()
