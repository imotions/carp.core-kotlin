package dk.cachet.carp.common.infrastructure.test

import dk.cachet.carp.common.application.data.Data
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@SerialName( StubDataTypes.STUB_DATA_TYPE_NAME )
data class StubData( val data: String = "Stub" ) : Data

@Serializable
@SerialName( StubDataTypes.STUB_DATA_POINT_TYPE_NAME )
data class StubDataPoint( val data: String = "Stub" ) : Data

@Serializable
@SerialName( StubDataTypes.STUB_DATA_TIME_SPAN_TYPE_NAME )
data class StubDataTimeSpan( val data: String = "Stub" ) : Data
