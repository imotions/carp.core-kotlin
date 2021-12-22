@file:JsExport

package dk.cachet.carp.deployments.application.users

import dk.cachet.carp.common.application.UUID
import kotlinx.serialization.Serializable
import kotlin.js.JsExport


/**
 * Provides information on the status of a participant in a study deployment.
 */
@Serializable
data class ParticipantStatus( val participantId: UUID, val assignedMasterDeviceRoleNames: Set<String> )
