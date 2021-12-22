@file:JsExport

package dk.cachet.carp.common.application.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.js.JsExport


/**
 * Holds data on whether a sensor requiring contact with skin is making proper contact.
 */
@Serializable
@SerialName( CarpDataTypes.SENSOR_SKIN_CONTACT_TYPE_NAME )
data class SensorSkinContact( val contact: Boolean ) : Data
