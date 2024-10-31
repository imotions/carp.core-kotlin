# CARP Core Framework

![Publish snapshots status](https://github.com/cph-cachet/carp.core-kotlin/actions/workflows/publish-snapshots.yml/badge.svg)

CARP Core is a software framework to help developers build research platforms to run studies involving _distributed data collection_.
It provides modules to define, deploy, and monitor research studies, and to collect data from multiple devices at multiple locations.

It is the result of a collaboration between [iMotions](https://imotions.com/) and the [Copenhagen Center for Health Technology (CACHET)](https://www.cachet.dk/).
Both use CARP Core to implement their respective research platforms: the [iMotions Mobile Research Platform](https://imotions.com/products/imotions-mobile/) and the [Copenhagen Research Platform (CARP)](https://carp.cachet.dk/).

Following [domain-driven design](https://en.wikipedia.org/wiki/Domain-driven_design), this project contains all domain models and application services for all CARP subsystems ([depicted below](#architecture)), not having any dependencies on concrete infrastructure.
As such, this project defines an **open standard for distributed data collection**, [available for Kotlin, the Java runtime, and JavaScript](#usage), which others can build upon to create their own infrastructure. 

Two key **design goals** differentiate this project from similar projects:
 
 - **Modularity**: Whether you want to set up your own entire infrastructure, customize part of it but integrate with externally hosted CARP webservices, or simply create an app which collects data locally on a smartphone,  you can 'pick and choose' the subsystems you are interested in and how to deploy them.
 Reference implementations [will be made available](https://github.com/cph-cachet/) (and hosted) by CACHET in the future. 
 - **Extensibility**: Where industry standards exist (such as [Open mHealth](https://www.openmhealth.org/)), the goal is to include them in CARP.
 However, we recognize that many study-specific requirements will always require parts of the infrastructure to be modified.
 Rather than having to fork the entire project and set up your own hosting, domain objects in the protocols subsystem [can be extended to describe study-specific requirements](docs/carp-protocols.md#extending-domain-objects).
 Your custom client (e.g., smartphone application) will need to know how to interpret them, but these additions _are transparent to, and compatible with, the rest of the framework_ when using the provided [built-in serializers](#serialization). 

## Table of Contents

- [Architecture](#architecture)
  - [Common](docs/carp-common.md)
    - [Data types](docs/carp-common.md#data-types)
    - [Device configurations](docs/carp-common.md#device-configurations)
    - [Sampling schemes and configurations](docs/carp-common.md#sampling-schemes-and-configurations)
    - [Task configurations](docs/carp-common.md#task-configurations)
    - [Trigger configurations](docs/carp-common.md#trigger-configurations)
  - [Protocols](docs/carp-protocols.md)
    - [Domain objects](docs/carp-protocols.md#domain-objects)
    - [Application services](docs/carp-protocols.md#application-services)
    - [Extending domain objects](docs/carp-protocols.md#extending-domain-objects)
  - [Studies](docs/carp-studies.md)
    - [Application services](docs/carp-studies.md#application-services)
  - [Deployments](docs/carp-deployments.md)
    - [Study and device deployment state](docs/carp-deployments.md#study-and-device-deployment-state)
    - [Application services](docs/carp-deployments.md#application-services)
  - [Clients](docs/carp-clients.md)
    - [Study state](docs/carp-clients.md#study-state)
  - [Data](docs/carp-data.md)
    - [Data streams](docs/carp-data.md#data-streams)
    - [Application services](docs/carp-data.md#application-services)
- [Infrastructure helpers](#infrastructure-helpers)
  - [Serialization](#serialization)
  - [Request objects](#request-objects)
  - [Application service versioning](#application-service-versioning)
  - [Authorization](#authorization)
  - [Stub classes](#stub-classes)
- [Usage](#usage)
  - [Example](#example)
- [Development](#development)
  - [Gradle tasks](#gradle-tasks)
  - [Release management](#release-management)
  - [Development checklists](#development-checklists)

## Architecture

![Subsystem decomposition](https://i.imgur.com/hEsTHNk.png)

- [**Protocols**](docs/carp-protocols.md): Supports management of study protocols—how a study should be run. Essentially, this subsystem has no _technical_ dependencies on any particular sensor technology or application as the containing study protocols merely describe why, when, and what data should be collected.

  [![Maven Central](https://maven-badges.herokuapp.com/maven-central/dk.cachet.carp.protocols/carp.protocols.core/badge.svg)](https://mvnrepository.com/artifact/dk.cachet.carp.protocols) [![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/dk.cachet.carp.protocols/carp.protocols.core?server=https%3A%2F%2Foss.sonatype.org)](https://oss.sonatype.org/content/repositories/snapshots/dk/cachet/carp/protocols/)

- [**Studies**](docs/carp-studies.md): Supports management of research studies, including the recruitment of participants and assigning metadata (e.g., contact information). This subsystem maps pseudonymized data (managed by the 'deployments' subsystem) to actual participants.

  [![Maven Central](https://maven-badges.herokuapp.com/maven-central/dk.cachet.carp.studies/carp.studies.core/badge.svg)](https://mvnrepository.com/artifact/dk.cachet.carp.studies) [![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/dk.cachet.carp.studies/carp.studies.core?server=https%3A%2F%2Foss.sonatype.org)](https://oss.sonatype.org/content/repositories/snapshots/dk/cachet/carp/studies/)

- [**Deployments**](docs/carp-deployments.md): Maps the information specified in a study protocol to runtime configurations used by 'client' subsystems to run the protocol on concrete devices (e.g., a smartphone) and allow researchers to monitor their state. To start collecting data, participants need to be invited, devices need to be registered, and consent needs to be given to collect the requested data.

  [![Maven Central](https://maven-badges.herokuapp.com/maven-central/dk.cachet.carp.deployments/carp.deployments.core/badge.svg)](https://mvnrepository.com/artifact/dk.cachet.carp.deployments) [![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/dk.cachet.carp.deployments/carp.deployments.core?server=https%3A%2F%2Foss.sonatype.org)](https://oss.sonatype.org/content/repositories/snapshots/dk/cachet/carp/deployments/)

- [**Clients**](docs/carp-clients.md): The runtime which performs the actual data collection on a device (e.g., desktop computer or smartphone). This subsystem contains reusable components which understand the runtime configuration derived from a study protocol by the 'deployment' subsystem. Integrations with sensors are loaded through a 'device data collector' plug-in system to decouple sensing—not part of core—from sensing logic.

   [![Maven Central](https://maven-badges.herokuapp.com/maven-central/dk.cachet.carp.clients/carp.clients.core/badge.svg?color=orange)](https://mvnrepository.com/artifact/dk.cachet.carp.clients) [![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/dk.cachet.carp.clients/carp.clients.core?server=https%3A%2F%2Foss.sonatype.org)](https://oss.sonatype.org/content/repositories/snapshots/dk/cachet/carp/clients/)

- [**Data**](docs/carp-data.md): Contains all pseudonymized data. In combination with the original study protocol, the full provenance of the data (when/why it was collected) is known. In combination with data from the 'studies' subsystem, the data can be linked back to individual participants.
  
  [![Maven Central](https://maven-badges.herokuapp.com/maven-central/dk.cachet.carp.data/carp.data.core/badge.svg)](https://mvnrepository.com/artifact/dk.cachet.carp.data) [![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/dk.cachet.carp.data/carp.data.core?server=https%3A%2F%2Foss.sonatype.org)](https://oss.sonatype.org/content/repositories/snapshots/dk/cachet/carp/data/)

- **Resources**: Contains a simple file store for resources (such as images, videos, and text documents) which can be referenced from within study protocols to be used during a study.
- **Analysis**: An analysis subsystem sits in between the data store and 'studies' subsystem, enabling common data analytics but also offering anonimity-preserving features such as k-anonymity.
- **Supporting subystems**:
   - [**Common**](docs/carp-common.md): Implements helper classes and base types relied upon by all subsystems.
Primarily, this contains the built-in types used to define study protocols
which subsequently get passed to the deployments and clients subsystem.
   
     [![Maven Central](https://maven-badges.herokuapp.com/maven-central/dk.cachet.carp.common/carp.common/badge.svg)](https://mvnrepository.com/artifact/dk.cachet.carp.common) [![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/dk.cachet.carp.common/carp.common?server=https%3A%2F%2Foss.sonatype.org)](https://oss.sonatype.org/content/repositories/snapshots/dk/cachet/carp/common/)
   - **carp.common.test**: Helper classes relied upon by test projects of all _core_ subsystems depending on types defined in _common_. 
     
     [![Maven Central](https://maven-badges.herokuapp.com/maven-central/dk.cachet.carp.common.test/carp.common.test/badge.svg)](https://mvnrepository.com/artifact/dk.cachet.carp.common.test) [![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/dk.cachet.carp.common.test/carp.common.test?server=https%3A%2F%2Foss.sonatype.org)](https://oss.sonatype.org/content/repositories/snapshots/dk/cachet/carp/common/test/)
   - **carp.test**: Helper classes relied upon by test projects of all subsystems. E.g., to disable tests specified in common part of projects for the JavaScript runtime only.
   
     [![Maven Central](https://maven-badges.herokuapp.com/maven-central/dk.cachet.carp.test/carp.test/badge.svg)](https://mvnrepository.com/artifact/dk.cachet.carp.test) [![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/dk.cachet.carp.test/carp.test?server=https%3A%2F%2Foss.sonatype.org)](https://oss.sonatype.org/content/repositories/snapshots/dk/cachet/carp/test/)
     
   - **carp.detekt**: Includes static code analysis extensions for [detekt](https://github.com/arturbosch/detekt), used when building this project to ensure conventions are followed.

Each of the subsystems expose [**application service interfaces with corresponding integration events**](carp.common/src/commonMain/kotlin/dk/cachet/carp/common/application/services).
Synchronous communication between subsystems happens via dependency injected application service interfaces,
which implementing infrastructures are expected to implement as remote procedure calls (RPCs).
Asynchronous communication between subsystems happens via an event bus,
which implementing infrastructures are expected to implement using a message queue which guarantees order for all `IntegrationEvent`'s sharing the same `aggregateId`.

Not all subsystems are implemented or complete yet.
Currently, this project contains a stable version of the protocols, studies, deployments, and data subsystems.
The client subsystem is still considered alpha and expected to change in the future.
The resources and analysis subsystem are envisioned later additions.

## Infrastructure helpers

Even though this library does not contain dependencies on concrete infrastructure, it does provide building blocks which greatly facilitate hosting the application services defined in this library as a distributed service and consuming them.
You are not required to use these, but they remove boilerplate code you would otherwise have to write.   

### Serialization

To facilitate easy exchange of requests across the different subsystems, all objects that are passed through application services are serializable to JSON using built-in serializers.
This works for both the Java runtime and JavaScript, which is achieved by relying on the [`kotlinx.serialization`](https://github.com/Kotlin/kotlinx.serialization) library and compiler plugin.
In fact, `kotlinx.serialization` [also supports other formats](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/formats.md), such as ProtoBuf and CBOR, but we have not tested those extensively.

In addition, domain objects which need to be persisted (aggregate roots) implement [the snapshot pattern](https://howtodoinjava.com/design-patterns/behavioral/memento-design-pattern/).
All snapshots are fully serializable to JSON, making it straightforward to store them in a document store.
But, if you prefer to use a relational database instead, you can call `consumeEvents()` to get all the modifications since the object was last stored.

Lastly, custom serializers to the default ones generated by `kotlinx.serialization` are provided for [extendable types used in study protocols](docs/carp-protocols.md#extending-domain-objects) (e.g., `DeviceConfiguration`).
These 'magic' serializers support deserializing extending types which are unknown at runtime, allowing you to access the base properties seamlessly.
Using the built-in serializers thus allows you to handle incoming requests and persistence of extending types you do not have available at compile time.
They are used by default in all objects that need to be serialized for data transfer or snapshot storage.
It is therefore recommended to use built-in serializers to store and transfer any objects containing study protocol information to get this type of extensibility for free.
More detailed information on how this works can be found in [the documentation on serialization for CARP developers](docs/serialization.md).

### Request objects

To help implement remote procedure calls (RPCs), each application service has matching polymorphic serializable 'request objects'.
For example, the "deployments" subsystem has a sealed class [`DeploymentServiceRequest`](carp.deployments.core/src/commonMain/kotlin/dk/cachet/carp/deployments/infrastructure/DeploymentServiceRequest.kt) and each subclass represents a request to `DeploymentService`.
Using these objects, all requests to a single application service can be handled by one endpoint using type checking.
We recommend [using a when expression](https://kotlinlang.org/docs/reference/sealed-classes.html) so that the compiler can verify whether you have handled all requests.

In addition, each request object can be executed by passing a matching application service to `invokeOn`.
This allows a centralized implementation for any incoming request object to an application service.
However, in practice you might want to perform additional actions depending on specific requests, e.g., [authorization which is currently not part of core](#authorization).

### Application service versioning

When using the default serializers for the provided request objects and integration events, you can get backwards compatible application services for free.
Each new CARP version will come with the necessary application service migration functionality for new minor API versions.
Clients that are on the same _major_ version as the backend will be able to use new hosted _minor_ versions of the API.

Each application service has a corresponding `ApplicationServiceApiMigrator`.
To get support for backwards compatible application services, you need to wire a call to `migrateRequest` into your infrastructure endpoints.
`MigratedRequest.invokeOn` can be used to execute the migrated request on the application service.

### Authorization

Currently, this library does not contain support for authorization.
Authorization needs to be implemented by concrete infrastructure.
However, CARP is designed with claim-based authorization in mind, and the documentation of application services in each of the subsystems describes a recommended implementation.
 
In a future release we might pass authorization as a dependent service to application services.

### Stub classes

Stub classes are available for the abstract domain objects defined in the common subsystem.
These can be used to write unit tests in which you are not interested in testing the behavior of specific device configurations, trigger configurations, etc., but rather how they are referenced from within a study protocol or deployment.

In addition, `String` manipulation functions are available to convert type names of protocol domain objects within a JSON string to 'unknown' type names. This supports testing deserialization of domain objects unknown at runtime, e.g., as defined in an application-specific client. See [the section on serialization](#serialization) for more details.

## Usage

This is a [multiplatform Kotlin library](https://kotlinlang.org/docs/reference/multiplatform.html) which targets both the **Java Runtime Environment (JRE)** and **JavaScript (JS)**.
Since this project _does not contain any infrastructure_, you need to include dependencies to the subsystems you want to implement infrastructure for and implement all application services, e.g. as a web service. We recommend reading the Kotlin documentation to see how to consume multiplatform libraries.

As this project progresses, we intend to include [native targets](https://kotlinlang.org/docs/reference/native-overview.html) as well, starting with iOS.

The releases are published to Maven. In case you want to use `SNAPSHOT` versions, use the following repository:

```groovy
maven { url "http://oss.sonatype.org/content/repositories/snapshots" }
```

### Example

The following shows how the subystems interact to create a study protocol, instantiate it as a study, and deploy it to a client.

<a name="example-protocols"></a>
**carp.protocols**: Example study protocol definition to collect GPS and step count on a smartphone which can be serialized to JSON:

```kotlin
// Create a new study protocol.
val ownerId = UUID.randomUUID()
val protocol = StudyProtocol( ownerId, "Track patient movement" )

// Define which devices are used for data collection.
val phone = Smartphone.create( "Patient's phone" )
{
    // Configure device-specific options, e.g., frequency to collect data at.
    defaultSamplingConfiguration {
        geolocation { batteryNormal { granularity = Granularity.Balanced } }
    }
}
protocol.addPrimaryDevice( phone )

// Define what needs to be measured, on which device, when.
val sensors = Smartphone.Sensors
val trackMovement = Smartphone.Tasks.BACKGROUND.create( "Track movement" ) {
    measures = listOf( sensors.GEOLOCATION.measure(), sensors.STEP_COUNT.measure() )
    description = "Track activity level and number of places visited per day."
}
protocol.addTaskControl( phone.atStartOfStudy().start( trackMovement, phone ) )

// JSON output of the study protocol, compatible with the rest of the CARP infrastructure.
val json: String = JSON.encodeToString( protocol.getSnapshot() )
```

<a name="example-studies"></a>
**carp.studies**: Example creation of a study based on a study protocol, and adding and deploying a single participant:

```kotlin
val (studyService, recruitmentService) = createEndpoints()

// Create a new study.
val ownerId = UUID.randomUUID()
var studyStatus: StudyStatus = studyService.createStudy( ownerId, "Example study" )
val studyId: UUID = studyStatus.studyId

// Let the study use the protocol from the 'carp.protocols' example above.
val trackPatientStudy: StudyProtocol = createExampleProtocol()
val protocolSnapshot: StudyProtocolSnapshot = trackPatientStudy.getSnapshot()
studyStatus = studyService.setProtocol( studyId, protocolSnapshot )

// Add a participant.
val email = EmailAddress( "participant@email.com" )
val participant: Participant = recruitmentService.addParticipant( studyId, email )

// Once all necessary study options have been configured, the study can go live.
if ( studyStatus is StudyStatus.Configuring && studyStatus.canGoLive )
{
    studyStatus = studyService.goLive( studyId )
}

// Once the study is live, you can 'deploy' it to participant's devices. They will be invited.
if ( studyStatus.canDeployToParticipants )
{
    // Create a 'participant group' with a single participant; `AssignedTo.All` assigns the "Patient's phone".
    val participation = AssignedParticipantRoles( participant.id, AssignedTo.All )
    val participantGroup = setOf( participation )

    val groupStatus: ParticipantGroupStatus = recruitmentService.inviteNewParticipantGroup( studyId, participantGroup )
    val isInvited = groupStatus is ParticipantGroupStatus.Invited // True.
}
```

<a name="example-deployments"></a>
**carp.deployments**: Most calls to this subsystem are abstracted away by the 'studies' and 'clients' subsystems, so you wouldn't call its endpoints directly. Example code which is called when a study is created and accessed by a client:

```kotlin
val deploymentService: DeploymentService = createDeploymentEndpoint()
val trackPatientStudy: StudyProtocol = createExampleProtocol()
val patientPhone: Smartphone = trackPatientStudy.primaryDevices.first() as Smartphone // "Patient's phone"

// This is called by `StudyService` when deploying a participant group.
val invitation = ParticipantInvitation(
    participantId = UUID.randomUUID(),
    assignedRoles = AssignedTo.All,
    identity = AccountIdentity.fromEmailAddress( "test@test.com" ),
    invitation = StudyInvitation( "Movement study", "This study tracks your movements." )
)
val studyDeploymentId = UUID.randomUUID()
deploymentService.createStudyDeployment(
    studyDeploymentId,
    trackPatientStudy.getSnapshot(),
    listOf( invitation )
)

// What comes after is similar to what is called by the client in `carp.client`:
// - Register the device to be deployed.
val registration = patientPhone.createRegistration()
var status = deploymentService.registerDevice( studyDeploymentId, patientPhone.roleName, registration )

// - Retrieve information on what to run and indicate the device is ready to collect the requested data.
val patientPhoneStatus: DeviceDeploymentStatus = status.getDeviceStatus( patientPhone )
if ( patientPhoneStatus.canObtainDeviceDeployment ) // True since there are no dependent devices.
{
    val deploymentInformation: PrimaryDeviceDeployment =
        deploymentService.getDeviceDeploymentFor( studyDeploymentId, patientPhone.roleName )
    val deployedOn: Instant = deploymentInformation.lastUpdatedOn // To verify correct deployment.
    deploymentService.deviceDeployed( studyDeploymentId, patientPhone.roleName, deployedOn )
}

// Now that all devices have been registered and deployed, the deployment is running.
status = deploymentService.getStudyDeploymentStatus( studyDeploymentId )
val isReady = status is StudyDeploymentStatus.Running // True.
```

<a name="example-data"></a>
**carp.data**: Calls to this subsystem are abstracted away by the 'deployments' subsystem and are planned to be abstracted away by the 'clients' subsystem.
Example code which is called once a deployment is running and data is subsequently uploaded by the client.

```kotlin
val dataStreamService: DataStreamService = createDataStreamEndpoint()
val studyDeploymentId: UUID = getStudyDeploymentId() // Provided by the 'deployments' subsystem.

// This is called by the `DeploymentsService` once the deployment starts running.
val device = "Patient's phone"
val geolocation = DataStreamsConfiguration.ExpectedDataStream( device, CarpDataTypes.GEOLOCATION.type )
val stepCount = DataStreamsConfiguration.ExpectedDataStream( device, CarpDataTypes.STEP_COUNT.type )
val configuration = DataStreamsConfiguration( studyDeploymentId, setOf( geolocation, stepCount ) )
dataStreamService.openDataStreams( configuration )

// Upload data from the client.
val geolocationData = MutableDataStreamSequence<Geolocation>(
    dataStream = dataStreamId<Geolocation>( studyDeploymentId, device ),
    firstSequenceId = 0,
    triggerIds = listOf( 0 ) // Provided by device deployment; maps to the `atStartOfStudy()` trigger.
)
val uploadData: DataStreamBatch = MutableDataStreamBatch().apply {
    appendSequence( geolocationData )
}
dataStreamService.appendToDataStreams( studyDeploymentId, uploadData )
```

<a name="example-client"></a>
**carp.client**: Example initialization of a smartphone client for the participant that got invited to the study in the 'studies' code sample above:

```kotlin
val (participationService, deploymentService) = createEndpoints()
val dataCollectorFactory = createDataCollectorFactory()

// Retrieve invitation to participate in the study using a specific device.
val account: Account = getLoggedInUser()
val invitation: ActiveParticipationInvitation =
    participationService.getActiveParticipationInvitations( account.id ).first()
val studyDeploymentId: UUID = invitation.participation.studyDeploymentId
val deviceToUse: String = invitation.assignedDevices.first().device.roleName // This matches "Patient's phone".

// Add the study to a client device manager.
val clientRepository = createRepository()
val client = SmartphoneClient( clientRepository, deploymentService, dataCollectorFactory )
client.configure {
    // Device-specific registration options can be accessed from here.
    // Depending on the device type, different options are available.
    // E.g., for a smartphone, a UUID deviceId is generated. To override this default:
    deviceId = "xxxxxxxxx"
    deviceDisplayName = "Pixel 6 Pro (Android 12)"
}
var status: StudyStatus = client.addStudy( studyDeploymentId, deviceToUse )

// Register connected devices in case needed.
if ( status is StudyStatus.RegisteringDevices )
{
    val connectedDevice = status.remainingDevicesToRegister.first()
    val connectedRegistration = connectedDevice.createRegistration()
    deploymentService.registerDevice( studyDeploymentId, connectedDevice.roleName, connectedRegistration )

    // Try deployment now that devices have been registered.
    status = client.tryDeployment( status.id )
    val isDeployed = status is StudyStatus.Running // True.
}
```

## Development

In case you want to contribute, please follow our [contribution guidelines](https://github.com/cph-cachet/carp.core-kotlin/blob/develop/CONTRIBUTING.md).

We recommend using IntelliJ IDEA 2023, as this is the development environment we use and is therefore fully tested.

- Open the project folder in IntelliJ 2023.
- Make sure Google Chrome is installed; JS unit tests are run on headless Chrome.
- In case you want to run TypeScript declaration tests (`verifyTsDeclarations`), install node.
- To build/test/publish, click "Edit Configurations" to add configurations for [the included Gradle tasks](#gradle-tasks), or run them from the Gradle tool window.

### Gradle tasks

For `carp.core-kotlin`:
- **build**: Builds the full project, for both runtimes, including running unit tests, but not code analysis.
- **jvmTest**: Test the full project on a JVM runtime using JUnit5.
- **jsTest**: Test the full project on a JavaScript runtime using a headless Chrome browser.
- **verifyTsDeclarations**: Verify whether the TypeScript declarations of all modules, defined in `typescript-declarations`, match the compiled JS sources and work at runtime.
- **detektPasses**: Run code analysis. Output will list failure in case code smells are detected.
- **publishToSonatype closeAndReleaseSonatypeStagingRepository**: Publish all projects to Maven using the version number specified in `ext.globalVersion`.
  This includes documentation, sources, and signing.
  For this to work you need to configure a `publish.properties` file with a signing signature and repository user in the project root folder.
  Preface with `setSnapshotVersion` task to publish to the snapshot repository, substituting the suffix of the version specified in `ext.globalVersion` with `-SNAPSHOT`.
  See main `build.gradle` for details.

### Release management

[Semantic versioning](https://semver.org/) is used for releases.
Backwards compatibility is assessed from the perspective of clients using an implementation of the framework,
as opposed to developers using the framework to implement an infrastructure. 
In other words, versioning is based on the exposed API (`application` namespaces), but not the domain used to implement infrastructures (`domain` namespaces).
Breaking changes between `minor` versions can occur in domain objects, including the need to do database migrations.

Module versions are configured in the main `build.gradle` in `ext.globalVersion` and `ext.clientsVersion`.

Workflows:
- Each push to `develop` triggers a snapshot release of the currently configured version.
- Each push to `master` triggers a release to Maven using the currently configured version.

Releases require a couple of manual steps:
- Before merging into `master`, make sure new versions are set in `build.gradle`.
  This should be done already in the last step, but you may decide to make a bigger version increment.
- Merge into master; **don't rebase**. Rebasing causes branch commit histories to diverge which complicates later releases and messes up the visible commit history with duplicate commits.
- Create a release tag on `master` with release notes.
- Add `javascript-typescript-sources.zip` and `rpc-examples.zip` assets to release.
  This should be automated in the future: [#371](https://github.com/cph-cachet/carp.core-kotlin/issues/371) and [#416](https://github.com/cph-cachet/carp.core-kotlin/issues/416) respectively.
- Bump versions on `develop` so that snapshot releases target the next version.

### Development checklists

When changes are made to CARP Core, various parts in the codebase sometimes need to be updated accordingly.
Generally speaking, failing tests will guide you as an attempt was made to catch omissions through automated tests.
But, recommended workflows for common new features/changes are documented in [development checklists](docs/development-checklists.md).
