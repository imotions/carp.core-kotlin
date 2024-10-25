@file:Suppress( "NON_EXPORTABLE_TYPE" )

package dk.cachet.carp.deployments.application

import dk.cachet.carp.common.application.devices.AnyDeviceConfiguration
import dk.cachet.carp.common.application.devices.DeviceRegistration
import kotlinx.serialization.*
import kotlin.js.JsExport


/**
 * Describes the status of a device, part of a study deployment.
 */
@Serializable
@JsExport
sealed class DeviceDeploymentStatus
{
    /**
     * The description of the device.
     */
    abstract val device: AnyDeviceConfiguration

    /**
     * Determines whether the device can be deployed by retrieving [PrimaryDeviceDeployment].
     * Not all primary devices necessarily need deployment; chained primary devices do not.
     */
    abstract val canBeDeployed: Boolean

    /**
     * Determines whether the device requires a device deployment, and if so,
     * whether the deployment configuration (to initialize the device environment) can be obtained.
     * This requires the specified device and all other primary devices it depends on to be registered.
     */
    val canObtainDeviceDeployment: Boolean
        get() = this is Deployed || (this is NotDeployed && this.remainingDevicesToRegisterToObtainDeployment.isEmpty())


    /**
     * A device deployment status which indicates the correct deployment has not been deployed yet.
     */
    sealed class NotDeployed : DeviceDeploymentStatus()
    {
        /**
         * Determines whether the device and all dependent devices have been registered successfully and is ready for deployment.
         */
        val isReadyForDeployment: Boolean
            get() = canBeDeployed && remainingDevicesToRegisterBeforeDeployment.isEmpty()

        /**
         * The role names of devices which need to be registered before the deployment information for this device can be obtained.
         */
        abstract val remainingDevicesToRegisterToObtainDeployment: Set<String>

        /**
         * The role names of devices which need to be registered before this device can be declared as successfully deployed.
         */
        abstract val remainingDevicesToRegisterBeforeDeployment: Set<String>
    }

    /**
     * A device deployment status which indicates the device has been registered.
     */
    sealed interface HasDeviceRegistration
    {
        /**
         * The last [DeviceRegistration] for this device.
         */
        val deviceRegistration: DeviceRegistration
    }

    /**
     * Device deployment status for when a device has not been registered.
     */
    @Serializable
    data class Unregistered(
        override val device: AnyDeviceConfiguration,
        override val canBeDeployed: Boolean,
        override val remainingDevicesToRegisterToObtainDeployment: Set<String>,
        override val remainingDevicesToRegisterBeforeDeployment: Set<String>
    ) : NotDeployed()

    /**
     * Device deployment status for when a device has been registered.
     */
    @Serializable
    data class Registered(
        override val device: AnyDeviceConfiguration,
        override val deviceRegistration: DeviceRegistration,
        override val canBeDeployed: Boolean,
        override val remainingDevicesToRegisterToObtainDeployment: Set<String>,
        override val remainingDevicesToRegisterBeforeDeployment: Set<String>
    ) : NotDeployed(), HasDeviceRegistration

    /**
     * Device deployment status when the device has retrieved its [PrimaryDeviceDeployment] and was able to load all the necessary plugins to execute the study.
     */
    @Serializable
    data class Deployed(
        override val device: AnyDeviceConfiguration,
        override val deviceRegistration: DeviceRegistration
    ) : DeviceDeploymentStatus(), HasDeviceRegistration
    {
        // All devices that have been deployed necessarily can be deployed.
        override val canBeDeployed = true
    }

    /**
     * Device deployment status when the device has previously been deployed correctly, but due to changes in device registrations needs to be redeployed.
     */
    @Serializable
    data class NeedsRedeployment(
        override val device: AnyDeviceConfiguration,
        override val deviceRegistration: DeviceRegistration,
        override val remainingDevicesToRegisterToObtainDeployment: Set<String>,
        override val remainingDevicesToRegisterBeforeDeployment: Set<String>
    ) : NotDeployed(), HasDeviceRegistration
    {
        // All devices that have been deployed necessarily can be deployed.
        override val canBeDeployed = true
    }
}
