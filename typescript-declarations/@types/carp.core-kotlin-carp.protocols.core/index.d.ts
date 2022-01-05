declare module 'carp.core-kotlin-carp.protocols.core'
{
    import { kotlin } from 'kotlin'
    import HashSet = kotlin.collections.HashSet
    import HashMap = kotlin.collections.HashMap

    import { kotlinx as kxd } from 'Kotlin-DateTime-library-kotlinx-datetime-js-legacy'
    import Instant = kxd.datetime.Instant

    import { dk as cdk } from 'carp.core-kotlin-carp.common'
    import UUID = cdk.cachet.carp.common.application.UUID
    import DeviceDescriptor = cdk.cachet.carp.common.application.devices.DeviceDescriptor
    import ParticipantAttribute = cdk.cachet.carp.common.application.users.ParticipantAttribute
    import TaskDescriptor = cdk.cachet.carp.common.application.tasks.TaskDescriptor
    import TaskControl = cdk.cachet.carp.common.application.triggers.TaskControl
    import Trigger = cdk.cachet.carp.common.application.triggers.Trigger


    namespace dk.cachet.carp.protocols.application
    {
        class ProtocolVersion
        {
            constructor( tag: string, date?: Instant )

            static get Companion(): ProtocolVersion$Companion

            readonly tag: string
            readonly date: Instant
        }
        interface ProtocolVersion$Companion { serializer(): any }


        class StudyProtocolSnapshot
        {
            // No manual initialization needed in TypeScript. Serialization should be used.
            private constructor()

            static get Companion(): StudyProtocolSnapshot$Companion

            readonly id: UUID
            readonly createdOn: Instant
            readonly ownerId: UUID
            readonly name: string
            readonly description: string
            readonly masterDevices: HashSet<DeviceDescriptor>
            readonly tasks: HashSet<TaskDescriptor>
            readonly triggers: HashMap<Number, Trigger>
            readonly taskControls: Set<TaskControl>
            readonly expectedParticipantData: HashSet<ParticipantAttribute>
        }
        interface StudyProtocolSnapshot$Companion { serializer(): any }
    }


    namespace dk.cachet.carp.protocols.infrastructure
    {
        import StudyProtocolSnapshot = dk.cachet.carp.protocols.application.StudyProtocolSnapshot

        
        abstract class ProtocolServiceRequest
        {
            static get Companion(): ProtocolServiceRequest$Companion
        }
        interface ProtocolServiceRequest$Companion { serializer(): any }

        namespace ProtocolServiceRequest
        {
            class Add extends ProtocolServiceRequest
            {
                constructor( protocol: StudyProtocolSnapshot, versionTag?: string )
            }
            class AddVersion extends ProtocolServiceRequest
            {
                constructor( protocol: StudyProtocolSnapshot, versionTag?: string )
            }
            class UpdateParticipantDataConfiguration extends ProtocolServiceRequest
            {
                constructor( protocolId: UUID, versionTag: string, expectedParticipantData: HashSet<ParticipantAttribute> )
            }
            class GetBy extends ProtocolServiceRequest
            {
                constructor( protocolId: UUID, versionTag?: string )
            }
            class GetAllForOwner extends ProtocolServiceRequest
            {
                constructor( ownerId: UUID )
            }
            class GetVersionHistoryFor extends ProtocolServiceRequest
            {
                constructor( protocolId: UUID )
            }
        }


        abstract class ProtocolFactoryServiceRequest
        {
            static get Companion(): ProtocolFactoryServiceRequest$Companion
        }
        interface ProtocolFactoryServiceRequest$Companion { serializer(): any }

        namespace ProtocolFactoryServiceRequest
        {
            class CreateCustomProtocol extends ProtocolFactoryServiceRequest
            {
                constructor( ownerId: UUID, name: string, customProtocol: string, description?: string | null )
            }
        }
    }
}
