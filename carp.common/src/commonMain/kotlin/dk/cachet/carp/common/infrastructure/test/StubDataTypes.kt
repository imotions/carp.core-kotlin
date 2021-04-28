package dk.cachet.carp.common.infrastructure.test

import dk.cachet.carp.common.application.data.CarpDataTypes
import dk.cachet.carp.common.application.data.DataType
import dk.cachet.carp.common.application.sampling.DataTypeSamplingScheme
import dk.cachet.carp.common.application.sampling.NoOptionsSamplingConfiguration
import dk.cachet.carp.common.application.sampling.NoOptionsSamplingConfigurationBuilder
import dk.cachet.carp.common.application.sampling.SamplingConfiguration


val STUB_DATA_TYPE: DataType = DataType( CarpDataTypes.CARP_NAMESPACE, "stub" )

class StubDataTypeSamplingScheme : DataTypeSamplingScheme<NoOptionsSamplingConfigurationBuilder>( STUB_DATA_TYPE )
{
    override fun createSamplingConfigurationBuilder(): NoOptionsSamplingConfigurationBuilder = NoOptionsSamplingConfigurationBuilder

    override fun isValid( configuration: SamplingConfiguration ): Boolean = configuration is NoOptionsSamplingConfiguration
}
