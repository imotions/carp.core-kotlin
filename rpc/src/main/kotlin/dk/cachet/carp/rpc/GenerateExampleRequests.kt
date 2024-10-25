@file:Suppress(
    "ArgumentListWrapping",
    "BooleanLiteralArgument",
    "MagicNumber",
    "MaximumLineLength",
    "TopLevelPropertyNaming"
)

package dk.cachet.carp.rpc

import dk.cachet.carp.common.application.*
import dk.cachet.carp.common.application.data.*
import dk.cachet.carp.common.application.data.input.*
import dk.cachet.carp.common.application.devices.*
import dk.cachet.carp.common.application.sampling.*
import dk.cachet.carp.common.application.services.ApplicationService
import dk.cachet.carp.common.application.tasks.*
import dk.cachet.carp.common.application.triggers.*
import dk.cachet.carp.common.application.users.*
import dk.cachet.carp.common.infrastructure.serialization.createDefaultJSON
import dk.cachet.carp.common.infrastructure.services.*
import dk.cachet.carp.data.application.*
import dk.cachet.carp.data.infrastructure.*
import dk.cachet.carp.deployments.application.*
import dk.cachet.carp.deployments.application.users.*
import dk.cachet.carp.deployments.infrastructure.*
import dk.cachet.carp.protocols.application.*
import dk.cachet.carp.protocols.domain.StudyProtocol
import dk.cachet.carp.protocols.domain.start
import dk.cachet.carp.protocols.infrastructure.*
import dk.cachet.carp.studies.application.*
import dk.cachet.carp.studies.application.users.*
import dk.cachet.carp.studies.infrastructure.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.reflect.KFunction
import kotlin.reflect.KSuspendFunction3
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds


/**
 * Generate a single [ExampleRequest] for each of the methods in the application service identified by [serviceInfo].
 */
fun generateExampleRequests( serviceInfo: ApplicationServiceInfo ): List<ExampleRequest>
{
    val requestMethods = serviceInfo.serviceKlass.methods
    val exampleRequests = exampleRequests.filter { it.key.javaMethod?.declaringClass == serviceInfo.serviceKlass }

    val json = Json( createDefaultJSON() ) { prettyPrint = true }

    return requestMethods.map { request ->
        // Retrieve example and verify whether it is valid.
        val requestName = serviceInfo.serviceName + "." + request.name
        val kotlinRequest = checkNotNull( request.kotlinFunction )
        val example = checkNotNull( exampleRequests[ kotlinRequest ] )
            { "No example request and response instances provided for $requestName." }
        check( example.request.matchesServiceRequest( kotlinRequest ) )
            { "Incorrect request instance provided for $requestName." }
        check( request.returnType.isInstance( example.response ) )
            { "Incorrect response instance provided for $requestName." }

        // Create example JSON.
        val exampleJson = json.encodeToJsonElement( serviceInfo.loggedRequestSerializer, example ) as JsonObject
        val requestObjectJson = json.encodeToString( checkNotNull( exampleJson[ "request" ] ) )
        val responseJson = json.encodeToString( checkNotNull( exampleJson[ "response" ] ) )

        ExampleRequest(
            request,
            ExampleRequest.JsonExample( example.request::class.java, requestObjectJson ),
            ExampleRequest.JsonExample( request.returnType, responseJson )
        )
    }
}

private fun <T : DeviceRegistration> T.setRegistrationCreatedOn( createdOn: Instant ): T
{
    val backingField = DeviceRegistration::registrationCreatedOn.javaField!!
    backingField.isAccessible = true
    backingField.set( this, createdOn )

    return this
}


// Example protocol with a single smartphone.
private val ownerId = UUID( "491f03fc-964b-4783-86a6-a528bbfe4e94" )
private val protocolId = UUID( "25fe92a5-0d52-4e37-8d05-31f347d72d3d" )
private val protocolCreatedOn = Instant.fromEpochSeconds( 1642503419 )
private val phone = Smartphone.create( "Participant's phone" ) {
    defaultSamplingConfiguration {
        geolocation { batteryNormal { granularity = Granularity.Detailed } }
    }
}
private val participantRole = ParticipantRole( "Participant", false )
private val bikeBeacon = AltBeacon( "Participant's bike", true )
private val measurePhoneMovement = BackgroundTask(
    "Monitor movement",
    listOf( Smartphone.Sensors.GEOLOCATION.measure { }, Smartphone.Sensors.STEP_COUNT.measure { } ),
    "Track step count and geolocation for one week.",
    7.days
)
private val measureBikeProximity = BackgroundTask(
    "Monitor proximity to bike",
    listOf( AltBeacon.Sensors.SIGNAL_STRENGTH.measure { } ),
    null,
    7.days
)
private val startOfStudyTrigger = phone.atStartOfStudy()
private val phoneProtocol = StudyProtocol(
    ownerId,
    "Nonmotorized transport study",
    "Track how much nonmotorized movement participants perform.",
    protocolId,
    protocolCreatedOn
).apply {
    addPrimaryDevice( phone )
    addParticipantRole( participantRole )
    addConnectedDevice( bikeBeacon, phone )
    addTaskControl( startOfStudyTrigger.start( measurePhoneMovement, phone ) )
    addTaskControl( startOfStudyTrigger.start( measureBikeProximity, bikeBeacon ) )
    applicationData = ApplicationData( "{\"uiTheme\": \"black\"}" )
}.getSnapshot()
private val startOfStudyTriggerId = phoneProtocol.triggers.entries.first { it.value == startOfStudyTrigger }.key
private val expectedParticipantData = setOf(
    ExpectedParticipantData(
        ParticipantAttribute.DefaultParticipantAttribute( CarpInputDataTypes.SEX ),
        AssignedTo.Roles( setOf( participantRole.role ) )
    )
)

// Example protocol factory protocols.
private val customProtocol = runBlocking {
    ProtocolFactoryServiceHost().createCustomProtocol(
        ownerId,
        "Fictional Company study",
        """{"collect-data": "heartrate, gps"}""",
        "Collect heartrate and GPS using Fictional Company's software."
    )
}.copy( id = UUID( "4d8c75c7-9604-48fa-8f9b-5ed3e4bd5df8" ), createdOn = protocolCreatedOn )

// Study matching the example protocol.
private val studyId = UUID( "791fd191-4279-482f-9ef5-5b4508efd959" )
private const val studyName = "Copenhagen transportation study"
private const val studyDescription = "Track how people walk/bike in Copenhagen."
private val studyCreatedOn = Instant.fromEpochSeconds( 1642503800 )
private val studyConfiguringStatus = StudyStatus.Configuring( studyId, studyName, studyCreatedOn, null, true, true, false, false )
private val studyLiveStatus = StudyStatus.Live( studyId, studyName, studyCreatedOn, phoneProtocol.id, false, false, true )

// Deployment data matching the example protocol.
private val deploymentId = UUID( "c9cc5317-48da-45f2-958e-58bc07f34681" )
private val deploymentIds = setOf( deploymentId, UUID( "d4a9bba4-860e-4c58-a356-8a91605dc1ee" ) )
private val deploymentCreatedOn = Instant.fromEpochSeconds( 1642504000 )
private val participantId = UUID( "32880e82-01c9-40cf-a6ed-17ff3348f251" )
private val participantAccount = EmailAccountIdentity( "boaty@mcboatface.com" )
private val participantAccountId = UUID( "ca60cb7f-de18-44b6-baf9-3c8e6a73005a" )
private val studyInvitation = StudyInvitation(
    studyName,
    "Participate in this study, which keeps track of how much you walk and bike!",
    ApplicationData( "{\"trialGroup\", \"A\"}" )
)
private val participantAssignedRoles = AssignedTo.Roles( setOf( participantRole.role ) )
private val participantInvitation = ParticipantInvitation(
    participantId,
    participantAssignedRoles,
    participantAccount,
    studyInvitation
)
private val participantData = ParticipantData(
    deploymentId,
    emptyMap(),
    listOf(
        ParticipantData.RoleData(
            participantRole.role,
            mapOf( CarpInputDataTypes.SEX to Sex.Male )
        )
    )
)
private val bikeBeaconPreregistration = bikeBeacon.createRegistration {
    manufacturerId = 0x118
    organizationId = UUID( "4e990957-0838-414c-bf25-2d391e2990b5" )
    majorId = 42
    minorId = 42
    additionalSpecifications = ApplicationData( """{"Model": "AnyBeacon B42"}""" )
}.setRegistrationCreatedOn( deploymentCreatedOn )
private val phoneRegistration = phone.createRegistration {
    deviceId = UUID( "fc7b41b0-e9e2-4b5d-8c3d-5119b556a3f0" ).toString()
}.setRegistrationCreatedOn( Instant.fromEpochSeconds( 1642514110 ) )
private val bikeBeaconStatus = DeviceDeploymentStatus.Registered( bikeBeacon, phoneRegistration, false, emptySet(), emptySet() )
private val participantStatusList = listOf(
    ParticipantStatus( participantId, participantAssignedRoles, setOf( phone.roleName ) )
)
private val invitedDeploymentStatus = StudyDeploymentStatus.Invited(
    deploymentCreatedOn,
    deploymentId,
    listOf(
        DeviceDeploymentStatus.Unregistered( phone, true, setOf( phone.roleName ), emptySet() ),
        bikeBeaconStatus
    ),
    participantStatusList,
    null
)
private val runningDeploymentStatus = StudyDeploymentStatus.Running(
    deploymentCreatedOn,
    deploymentId,
    listOf(
        DeviceDeploymentStatus.Deployed( phone, phoneRegistration ),
        bikeBeaconStatus
    ),
    participantStatusList,
    Instant.fromEpochSeconds( 1642504500 )
)
private val stoppedDeploymentStatus = StudyDeploymentStatus.Stopped(
    deploymentCreatedOn,
    deploymentId,
    listOf(
        DeviceDeploymentStatus.Deployed( phone, phoneRegistration ),
        bikeBeaconStatus
    ),
    participantStatusList,
    runningDeploymentStatus.startedOn,
    Instant.fromEpochSeconds( 1642506000 )
)
private val participants = setOf( Participant( participantAccount, participantId ) )
private val participantGroupInvitedOn = Instant.fromEpochSeconds( 1642514010 )
private val phoneDeviceDeployment = PrimaryDeviceDeployment(
    phone,
    phoneRegistration,
    setOf( bikeBeacon ),
    mapOf( bikeBeacon.roleName to bikeBeaconPreregistration ),
    setOf( measurePhoneMovement, measureBikeProximity ),
    mapOf( startOfStudyTriggerId to phone.atStartOfStudy() ),
    setOf(
        TaskControl( startOfStudyTriggerId, measurePhoneMovement.name, phone.roleName, TaskControl.Control.Start ),
        TaskControl( startOfStudyTriggerId, measureBikeProximity.name, bikeBeacon.roleName, TaskControl.Control.Start )
    ),
    expectedParticipantData,
    phoneProtocol.applicationData
)

// Data matching the example protocol.
private val phoneGeoDataStream = dataStreamId<Geolocation>( deploymentId, phone.roleName )
private val phoneStepsDataStream = dataStreamId<StepCount>( deploymentId, phone.roleName )
private val expectedDataStreams = setOf(
    DataStreamsConfiguration.ExpectedDataStream.fromDataStreamId( phoneGeoDataStream ),
    DataStreamsConfiguration.ExpectedDataStream.fromDataStreamId( phoneStepsDataStream )
)
private val geoDataSequence =
    MutableDataStreamSequence<Geolocation>( phoneGeoDataStream, 0, listOf( startOfStudyTriggerId ) ).apply {
        appendMeasurements( measurement( Geolocation( 55.68061908805645, 12.582050313435703, sensorSpecificData = SignalStrength( 0 ) ), 1642505045000000L ) )
        appendMeasurements( measurement( Geolocation( 55.680802203873114, 12.581802212861367 ), 1642505144000000L ) )
    }
private val stepsDataSequence =
    MutableDataStreamSequence<StepCount>( phoneStepsDataStream, 0, listOf( startOfStudyTriggerId ) ).apply {
        appendMeasurements( measurement( StepCount( 0 ), 1642505045000000L ) )
        appendMeasurements( measurement( StepCount( 30 ), 1642505144000000L ) )
    }
private val phoneDataStreamBatch = MutableDataStreamBatch().apply {
    appendSequence( geoDataSequence )
    appendSequence( stepsDataSequence )
}


private fun <TService : ApplicationService<TService, *>, TResponse> example(
    request: ApplicationServiceRequest<TService, TResponse>,
    response: Any? = Unit
) = LoggedRequest.Succeeded( request, emptyList(), emptyList(), response )

private val exampleRequests: Map<KFunction<*>, LoggedRequest.Succeeded<*>> = mapOf(
    // ProtocolService
    ProtocolService::add to example(
        request = ProtocolServiceRequest.Add( phoneProtocol, "Version 1" )
    ),
    ProtocolService::addVersion to example(
        request = ProtocolServiceRequest.AddVersion( phoneProtocol.copy( name = "Walking/biking study" ), "Version 2: new name" )
    ),
    ProtocolService::updateParticipantDataConfiguration to example(
        request = ProtocolServiceRequest.UpdateParticipantDataConfiguration( protocolId, "Version 3: ask participant data", expectedParticipantData ),
        response = phoneProtocol.copy( expectedParticipantData = expectedParticipantData )
    ),
    ProtocolService::getBy to example(
        request = ProtocolServiceRequest.GetBy( protocolId, "Version 1" ),
        response = phoneProtocol
    ),
    ProtocolService::getAllForOwner to example(
        request = ProtocolServiceRequest.GetAllForOwner( ownerId ),
        response = listOf( phoneProtocol )
    ),
    ProtocolService::getVersionHistoryFor to example(
        request = ProtocolServiceRequest.GetVersionHistoryFor( protocolId ),
        response = listOf(
            ProtocolVersion( "Version 1", protocolCreatedOn ),
            ProtocolVersion( "Version 2: new name", protocolCreatedOn + 10.seconds ),
            ProtocolVersion( "Version 3: ask participant data", protocolCreatedOn + 20.seconds )
        )
    ),

    // ProtocolFactoryService
    ProtocolFactoryService::createCustomProtocol to example(
        request = ProtocolFactoryServiceRequest.CreateCustomProtocol(
            ownerId,
            customProtocol.name,
            customProtocol.tasks.filterIsInstance<CustomProtocolTask>().single().studyProtocol,
            customProtocol.description
        ),
        response = customProtocol
    ),

    // StudyService
    StudyService::createStudy to example(
        request = StudyServiceRequest.CreateStudy( ownerId, studyName, studyDescription, studyInvitation ),
        response = studyConfiguringStatus
    ),
    StudyService::setInternalDescription to example(
        request = StudyServiceRequest.SetInternalDescription( studyId, "Copenhagen/Denmark transportation study", studyDescription ),
        response = studyConfiguringStatus.copy( name = "Copenhagen/Denmark transportation study" )
    ),
    StudyService::getStudyDetails to example(
        request = StudyServiceRequest.GetStudyDetails( studyId ),
        response = StudyDetails( studyId, ownerId, studyName, studyCreatedOn, studyDescription, studyInvitation, phoneProtocol )
    ),
    StudyService::getStudyStatus to example(
        request = StudyServiceRequest.GetStudyStatus( studyId ),
        response = studyLiveStatus
    ),
    StudyService::getStudiesOverview to example(
        request = StudyServiceRequest.GetStudiesOverview( ownerId ),
        response = listOf(
            studyConfiguringStatus,
            StudyStatus.Live( UUID( "3566eb9c-1d2f-4ed9-bf8a-8ea43638773d" ), "Heartrate study", studyCreatedOn, phoneProtocol.id, false, false, true )
        )
    ),
    StudyService::setInvitation to example(
        request = StudyServiceRequest.SetInvitation( studyId, studyInvitation ),
        response = studyConfiguringStatus
    ),
    StudyService::setProtocol to example(
        request = StudyServiceRequest.SetProtocol( studyId, phoneProtocol ),
        response = StudyStatus.Configuring( studyId, studyName, studyCreatedOn, phoneProtocol.id, true, true, false, true )
    ),
    StudyService::removeProtocol to example(
        request = StudyServiceRequest.RemoveProtocol( studyId ),
        response = StudyStatus.Configuring( studyId, studyName, studyCreatedOn, null, true, true, false, false )
    ),
    StudyService::goLive to example(
        request = StudyServiceRequest.GoLive( studyId ),
        response = studyLiveStatus
    ),
    StudyService::remove to example(
        request = StudyServiceRequest.Remove( studyId ),
        response = true
    ),

    // RecruitmentService
    run<KSuspendFunction3<RecruitmentService, UUID, EmailAddress, Participant>> {
        RecruitmentService::addParticipant
    } to example(
        request = RecruitmentServiceRequest.AddParticipantByEmailAddress( studyId, participantAccount.emailAddress ),
        response = Participant( participantAccount, participantId )
    ),
    run<KSuspendFunction3<RecruitmentService, UUID, Username, Participant>> {
        RecruitmentService::addParticipant
    } to example(
        request = RecruitmentServiceRequest.AddParticipantByUsername( studyId, Username( "John Doe" ) ),
        response = Participant( UsernameAccountIdentity( "John Doe" ), UUID( "d7436912-ac9f-4f9b-a29e-376af8a0fbb4" ) )
    ),
    RecruitmentService::getParticipant to example(
        request = RecruitmentServiceRequest.GetParticipant( studyId, participantId ),
        response = Participant( participantAccount, participantId )
    ),
    RecruitmentService::getParticipants to example(
        request = RecruitmentServiceRequest.GetParticipants( studyId ),
        response = listOf(
            Participant( participantAccount, participantId ),
            Participant( UsernameAccountIdentity( "John Doe" ), UUID( "d7436912-ac9f-4f9b-a29e-376af8a0fbb4" ) )
        )
    ),
    RecruitmentService::inviteNewParticipantGroup to example(
        request = RecruitmentServiceRequest.InviteNewParticipantGroup(
            studyId,
            setOf( AssignedParticipantRoles( participantId, participantAssignedRoles ) )
        ),
        response = ParticipantGroupStatus.Invited( deploymentId, participants, participantGroupInvitedOn, invitedDeploymentStatus )
    ),
    RecruitmentService::getParticipantGroupStatusList to example(
        request = RecruitmentServiceRequest.GetParticipantGroupStatusList( studyId ),
        response = listOf( ParticipantGroupStatus.Running( deploymentId, participants, participantGroupInvitedOn, runningDeploymentStatus, runningDeploymentStatus.startedOn ) )
    ),
    RecruitmentService::stopParticipantGroup to example(
        request = RecruitmentServiceRequest.StopParticipantGroup( studyId, deploymentId ),
        response = ParticipantGroupStatus.Stopped( deploymentId, participants, participantGroupInvitedOn, stoppedDeploymentStatus, stoppedDeploymentStatus.startedOn, stoppedDeploymentStatus.stoppedOn )
    ),

    // DeploymentService
    DeploymentService::createStudyDeployment to example(
        request = DeploymentServiceRequest.CreateStudyDeployment(
            deploymentId,
            phoneProtocol,
            listOf( participantInvitation ),
            mapOf( bikeBeacon.roleName to bikeBeaconPreregistration )
        ),
        response = invitedDeploymentStatus
    ),
    DeploymentService::removeStudyDeployments to example(
        request = DeploymentServiceRequest.RemoveStudyDeployments( deploymentIds ),
        response = setOf( deploymentId )
    ),
    DeploymentService::getStudyDeploymentStatus to example(
        request = DeploymentServiceRequest.GetStudyDeploymentStatus( deploymentId ),
        response = invitedDeploymentStatus
    ),
    DeploymentService::getStudyDeploymentStatusList to example(
        request = DeploymentServiceRequest.GetStudyDeploymentStatusList( setOf( deploymentId ) ),
        response = listOf( invitedDeploymentStatus )
    ),
    DeploymentService::registerDevice to example(
        request = DeploymentServiceRequest.RegisterDevice( deploymentId, phone.roleName, phoneRegistration ),
        response = StudyDeploymentStatus.DeployingDevices(
            deploymentCreatedOn,
            deploymentId,
            listOf(
                DeviceDeploymentStatus.Registered( phone, phoneRegistration, true, emptySet(), emptySet() ),
                bikeBeaconStatus
            ),
            participantStatusList,
            null
        )
    ),
    DeploymentService::unregisterDevice to example(
        request = DeploymentServiceRequest.UnregisterDevice( deploymentId, phone.roleName ),
        response = invitedDeploymentStatus
    ),
    DeploymentService::getDeviceDeploymentFor to example(
        request = DeploymentServiceRequest.GetDeviceDeploymentFor( deploymentId, phone.roleName ),
        response = phoneDeviceDeployment
    ),
    DeploymentService::deviceDeployed to example(
        request = DeploymentServiceRequest.DeviceDeployed( deploymentId, phone.roleName, phoneDeviceDeployment.lastUpdatedOn ),
        response = runningDeploymentStatus
    ),
    DeploymentService::stop to example(
        request = DeploymentServiceRequest.Stop( deploymentId ),
        response = stoppedDeploymentStatus
    ),

    // ParticipationService
    ParticipationService::getActiveParticipationInvitations to example(
        request = ParticipationServiceRequest.GetActiveParticipationInvitations( participantAccountId ),
        response = setOf(
            ActiveParticipationInvitation(
                Participation( deploymentId, participantAssignedRoles, participantId ),
                studyInvitation,
                setOf( AssignedPrimaryDevice( phone ) )
            )
        )
    ),
    ParticipationService::getParticipantData to example(
        request = ParticipationServiceRequest.GetParticipantData( deploymentId ),
        response = participantData
    ),
    ParticipationService::getParticipantDataList to example(
        request = ParticipationServiceRequest.GetParticipantDataList( setOf( deploymentId ) ),
        response = listOf( participantData )
    ),
    ParticipationService::setParticipantData to example(
        request = ParticipationServiceRequest.SetParticipantData( deploymentId, participantData.roles.first().data, participantRole.role ),
        response = participantData
    ),

    // DataStreamService
    DataStreamService::openDataStreams to example(
        request = DataStreamServiceRequest.OpenDataStreams( DataStreamsConfiguration( deploymentId, expectedDataStreams ) )
    ),
    DataStreamService::appendToDataStreams to example(
        request = DataStreamServiceRequest.AppendToDataStreams( deploymentId, phoneDataStreamBatch )
    ),
    DataStreamService::getDataStream to example(
        request = DataStreamServiceRequest.GetDataStream( phoneGeoDataStream, 0, 100 ),
        response = MutableDataStreamBatch().apply { appendSequence( geoDataSequence ) }
    ),
    DataStreamService::closeDataStreams to example(
        request = DataStreamServiceRequest.CloseDataStreams( deploymentIds )
    ),
    DataStreamService::removeDataStreams to example(
        request = DataStreamServiceRequest.RemoveDataStreams( deploymentIds ),
        response = deploymentIds
    )
)
