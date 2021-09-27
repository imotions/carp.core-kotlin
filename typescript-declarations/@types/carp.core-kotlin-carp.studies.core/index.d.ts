declare module 'carp.core-kotlin-carp.studies.core'
{
    import { kotlin } from 'kotlin'
    import ArrayList = kotlin.collections.ArrayList
    import HashSet = kotlin.collections.HashSet

    import { kotlinx as kxd } from 'Kotlin-DateTime-library-kotlinx-datetime-js-legacy'
    import Instant = kxd.datetime.Instant

    import { dk as cdk } from 'carp.core-kotlin-carp.common'
    import EmailAddress = cdk.cachet.carp.common.application.EmailAddress
    import UUID = cdk.cachet.carp.common.application.UUID
    import AccountIdentity = cdk.cachet.carp.common.application.users.AccountIdentity

    import { dk as ddk } from 'carp.core-kotlin-carp.deployments.core'
    import StudyDeploymentStatus = ddk.cachet.carp.deployments.application.StudyDeploymentStatus
    import StudyInvitation = ddk.cachet.carp.deployments.application.users.StudyInvitation

    import { dk as pdk } from 'carp.core-kotlin-carp.protocols.core'
    import StudyProtocolSnapshot = pdk.cachet.carp.protocols.application.StudyProtocolSnapshot


    namespace dk.cachet.carp.studies.application
    {
        import StudyOwner = dk.cachet.carp.studies.application.users.StudyOwner


        class StudyDetails
        {
            constructor(
                studyId: UUID, studyOwner: StudyOwner, name: string, createdOn: Instant,
                description: string | null,
                invitation: StudyInvitation,
                protocolSnapshot: StudyProtocolSnapshot | null )

            static get Companion(): StudyDetails$Companion

            readonly studyId: UUID
            readonly studyOwner: StudyOwner
            readonly name: string
            readonly createdOn: Instant
            readonly description: string | null
            readonly invitation: StudyInvitation
            readonly protocolSnapshot: StudyProtocolSnapshot | null
        }
        interface StudyDetails$Companion { serializer(): any }


        abstract class StudyStatus
        {
            readonly studyId: UUID
            readonly name: string
            readonly createdOn: Instant
            readonly canSetInvitation: boolean
            readonly canSetStudyProtocol: boolean
            readonly canDeployToParticipants: boolean

            static get Companion(): StudyStatus$Companion
        }
        interface StudyStatus$Companion { serializer(): any }

        namespace StudyStatus
        {
            class Configuring extends StudyStatus
            {
                constructor(
                    studyId: UUID, name: string, createdOn: Instant,
                    canSetInvitation: boolean,
                    canSetStudyProtocol: boolean,
                    canDeployToParticipants: boolean,
                    canGoLive: boolean )
    
                readonly canGoLive: boolean
            }
            class Live extends StudyStatus
            {
                constructor(
                    studyId: UUID, name: string, createdOn: Instant,
                    canSetInvitation: boolean,
                    canSetStudyProtocol: boolean,
                    canDeployToParticipants: boolean )
            }
        }
    }


    namespace dk.cachet.carp.studies.application.users
    {
        class AssignParticipantDevices
        {
            constructor( participantId: UUID, masterDeviceRoleNames: HashSet<string> )

            static get Companion(): AssignParticipantDevices$Companion

            readonly participantId: UUID
            readonly masterDeviceRoleNames: HashSet<string>
        }
        function participantIds_ttprz$( assignedGroup: ArrayList<AssignParticipantDevices> ): HashSet<UUID>
        function deviceRoles_ttprz$( assignedGroup: ArrayList<AssignParticipantDevices> ): HashSet<string>
        interface AssignParticipantDevices$Companion { serializer(): any }


        class Participant
        {
            constructor( accountIdentity: AccountIdentity, id?: UUID )

            static get Companion(): Participant$Companion

            readonly accountIdentity: AccountIdentity
            readonly id: UUID
        }
        interface Participant$Companion { serializer(): any }


        class ParticipantGroupStatus
        {
            constructor( studyDeploymentStatus: StudyDeploymentStatus, invitedOn: Instant, participants: HashSet<Participant> )

            static get Companion(): ParticipantGroupStatus$Companion

            readonly studyDeploymentStatus: StudyDeploymentStatus
            readonly invitedOn: Instant
            readonly participants: HashSet<Participant>
        }
        interface ParticipantGroupStatus$Companion { serializer(): any }


        class StudyOwner
        {
            constructor( id?: UUID )

            static get Companion(): StudyOwner$Companion

            readonly id: UUID
        }
        interface StudyOwner$Companion { serializer(): any }
    }


    namespace dk.cachet.carp.studies.infrastructure
    {
        import AssignParticipantDevices = dk.cachet.carp.studies.application.users.AssignParticipantDevices
        import StudyOwner = dk.cachet.carp.studies.application.users.StudyOwner


        abstract class StudyServiceRequest
        {
            static get Companion(): StudyServiceRequest$Companion
        }
        interface StudyServiceRequest$Companion { serializer(): any }

        namespace StudyServiceRequest
        {
            class CreateStudy extends StudyServiceRequest
            {
                constructor( owner: StudyOwner, name: string, description?: string | null, invitation?: StudyInvitation )
            }
            class SetInternalDescription extends StudyServiceRequest
            {
                constructor( studyId: UUID, name: string, description: string | null )
            }
            class GetStudyDetails extends StudyServiceRequest
            {
                constructor( studyId: UUID )
            }
            class GetStudyStatus extends StudyServiceRequest
            {
                constructor( studyId: UUID )
            }
            class GetStudiesOverview extends StudyServiceRequest
            {
                constructor( owner: StudyOwner )
            }
            class SetInvitation extends StudyServiceRequest
            {
                constructor( studyId: UUID, invitation: StudyInvitation )
            }
            class SetProtocol extends StudyServiceRequest
            {
                constructor( studyId: UUID, protocol: StudyProtocolSnapshot )
            }
            class GoLive extends StudyServiceRequest
            {
                constructor( studyId: UUID )
            }
            class Remove extends StudyServiceRequest
            {
                constructor( studyId: UUID )
            }
        }


        abstract class RecruitmentServiceRequest
        {
            static get Companion(): RecruitmentServiceRequest$Companion
        }
        interface RecruitmentServiceRequest$Companion { serializer(): any }

        namespace RecruitmentServiceRequest
        {
            class AddParticipant extends RecruitmentServiceRequest
            {
                constructor( studyId: UUID, email: EmailAddress )
            }
            class GetParticipant extends RecruitmentServiceRequest
            {
                constructor( studyId: UUID, participantId: UUID )
            }
            class GetParticipants extends RecruitmentServiceRequest
            {
                constructor( studyId: UUID )
            }
            class DeployParticipantGroup extends RecruitmentServiceRequest
            {
                constructor( studyId: UUID, group: HashSet<AssignParticipantDevices> )
            }
            class GetParticipantGroupStatusList extends RecruitmentServiceRequest
            {
                constructor( studyId: UUID )
            }
            class StopParticipantGroup extends RecruitmentServiceRequest
            {
                constructor( studyId: UUID, groupId: UUID )
            }
        }
    }
}
