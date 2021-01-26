package dk.cachet.carp.studies.domain.users

import dk.cachet.carp.common.EmailAddress
import dk.cachet.carp.common.UUID
import dk.cachet.carp.common.ddd.AggregateRoot
import dk.cachet.carp.common.ddd.DomainEvent
import dk.cachet.carp.common.users.EmailAccountIdentity
import dk.cachet.carp.deployment.domain.users.Participation
import dk.cachet.carp.deployment.domain.users.StudyInvitation
import dk.cachet.carp.protocols.domain.StudyProtocolSnapshot


/**
 * Represents a set of [participants] recruited for a [Study] identified by [studyId].
 */
class Recruitment( val studyId: UUID ) :
    AggregateRoot<Recruitment, RecruitmentSnapshot, Recruitment.Event>()
{
    sealed class Event : DomainEvent()
    {
        data class ParticipantAdded( val participant: Participant ) : Event()
        data class ParticipationAdded( val studyDeploymentId: UUID, val participation: DeanonymizedParticipation ) : Event()
    }


    companion object
    {
        fun fromSnapshot( snapshot: RecruitmentSnapshot ): Recruitment
        {
            val recruitment = Recruitment( snapshot.studyId )
            recruitment.creationDate = snapshot.creationDate
            if ( snapshot.studyProtocol != null && snapshot.invitation != null )
            {
                recruitment.readyForDeployment( snapshot.studyProtocol, snapshot.invitation )
            }
            snapshot.participants.forEach { recruitment._participants.add( it ) }
            for ( p in snapshot.participations )
            {
                recruitment._participations[ p.key ] = p.value.toMutableSet()
            }

            return recruitment
        }
    }


    // We don't expect massive amounts of participants, so storing them within recruitment is fine for now.
    private val _participants: MutableSet<Participant> = mutableSetOf()

    /**
     * The participants which are part of this [Recruitment].
     */
    val participants: Set<Participant>
        get() = _participants.toSet()

    /**
     * Add a [Participant] identified by the specified [email] address.
     * In case the [email] was already added before, the same [Participant] is returned.
     */
    fun addParticipant( email: EmailAddress ): Participant
    {
        // Verify whether participant was already added.
        val identity = EmailAccountIdentity( email )
        var participant = _participants.firstOrNull { it.accountIdentity == identity }

        // Add new participant in case it was not added before.
        if ( participant == null )
        {
            participant = Participant( identity )
            _participants.add( participant )
            event( Event.ParticipantAdded( participant ) )
        }

        return participant
    }

    private var studyProtocol: StudyProtocolSnapshot? = null
    private var invitation: StudyInvitation? = null

    /**
     * Lock in the [protocol] which participants in this recruitment can participate in,
     * and the [invitation] which is sent to them once they are deployed.
     */
    fun readyForDeployment( protocol: StudyProtocolSnapshot, invitation: StudyInvitation )
    {
        this.studyProtocol = protocol
        this.invitation = invitation
    }

    /**
     * Get the status (serializable) of this [Recruitment].
     */
    fun getStatus(): RecruitmentStatus
    {
        val protocol = studyProtocol
        val invitation = invitation
        return if ( protocol != null && invitation != null ) RecruitmentStatus.ReadyForDeployment( protocol, invitation )
            else RecruitmentStatus.AwaitingStudyToGoLive
    }

    /**
     * Per study deployment ID, the set of participants that participate in it.
     * TODO: Maybe this should be kept private and be replaced with clearer helper functions (e.g., getStudyDeploymentIds).
     */
    val participations: Map<UUID, Set<DeanonymizedParticipation>>
        get() = _participations

    private val _participations: MutableMap<UUID, MutableSet<DeanonymizedParticipation>> = mutableMapOf()

    /**
     * Specify that a [Participation] has been created for a [Participant] in this recruitment.
     *
     * @throws IllegalStateException when the study is not yet ready for deployment.
     */
    fun addParticipation( studyDeploymentId: UUID, participation: DeanonymizedParticipation )
    {
        check( getStatus() is RecruitmentStatus.ReadyForDeployment ) { "The study is not yet ready for deployment." }

        _participations
            .getOrPut( studyDeploymentId ) { mutableSetOf() }
            .add( participation )
            .eventIf( true ) { Event.ParticipationAdded( studyDeploymentId, participation ) }
    }

    /**
     * Get all [DeanonymizedParticipation]s for a specific [studyDeploymentId].
     *
     * @throws IllegalArgumentException when the given [studyDeploymentId] is not part of this recruitment.
     */
    fun getParticipations( studyDeploymentId: UUID ): Set<DeanonymizedParticipation>
    {
        val participations: Set<DeanonymizedParticipation> = _participations.getOrElse( studyDeploymentId ) { emptySet() }
        require( participations.isNotEmpty() ) { "The specified study deployment ID is not part of this recruitment." }

        return participations
    }

    /**
     * Get a serializable snapshot of the current state of this [Recruitment].
     */
    override fun getSnapshot(): RecruitmentSnapshot = RecruitmentSnapshot.fromParticipantRecruitment( this )
}
