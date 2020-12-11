package dk.cachet.carp.protocols.application

import dk.cachet.carp.protocols.domain.ProtocolOwner
import dk.cachet.carp.protocols.domain.StudyProtocol
import dk.cachet.carp.protocols.domain.StudyProtocolSnapshot
import dk.cachet.carp.protocols.infrastructure.test.StubMasterDeviceDescriptor
import dk.cachet.carp.protocols.infrastructure.test.createEmptyProtocol
import dk.cachet.carp.test.runSuspendTest
import kotlin.test.*


/**
 * Tests for implementations of [ProtocolService].
 */
interface ProtocolServiceTest
{
    /**
     * Create a study service to be used in the tests.
     */
    fun createService(): ProtocolService


    @Test
    fun add_protocol_and_retrieving_it_succeeds() = runSuspendTest {
        val service = createService()
        val protocol = createEmptyProtocol()
        val snapshot = protocol.getSnapshot()

        service.add( snapshot, "Initial" )
        val retrieved = service.getBy( protocol.owner, protocol.name, "Initial" )
        assertEquals( snapshot, retrieved )
    }

    @Test
    fun add_fails_when_protocol_already_exists() = runSuspendTest {
        val service = createService()
        val protocol = createEmptyProtocol().getSnapshot()
        service.add( protocol )

        assertFailsWith<IllegalArgumentException> { service.add( protocol ) }
    }

    @Test
    fun addVersion_succeeds() = runSuspendTest {
        val service = createService()
        val protocol = createEmptyProtocol()
        service.add( protocol.getSnapshot() )

        modifyProtocol( protocol )
        val version2Snapshot = protocol.getSnapshot()
        service.addVersion( version2Snapshot, "Version 2" )

        val retrieved = service.getBy( protocol.owner, protocol.name, "Version 2" )
        assertEquals( version2Snapshot, retrieved )
    }

    @Test
    fun addVersion_fails_when_protocol_does_not_exist() = runSuspendTest {
        val service = createService()
        val protocol = createEmptyProtocol()

        assertFailsWith<IllegalArgumentException> { service.addVersion( protocol.getSnapshot() ) }
    }

    @Test
    fun addVersion_fails_when_version_tag_in_use() = runSuspendTest {
        val service = createService()
        val protocol = createEmptyProtocol()
        service.add( protocol.getSnapshot(), "In use" )
        modifyProtocol( protocol )

        val newSnapshot = protocol.getSnapshot()
        assertFailsWith<IllegalArgumentException> { service.addVersion( newSnapshot, "In use" ) }
    }

    @Test
    fun add_and_addVersion_fail_when_protocol_is_invalid() = runSuspendTest {
        val service = createService()
        val protocol = createEmptyProtocol()
        service.add( protocol.getSnapshot() )

        val invalidSnapshot = protocol.getSnapshot().copy(
            triggeredTasks = listOf(
                StudyProtocolSnapshot.TriggeredTask( 0, "Non-existing", "Not a device" )
            )
        )

        assertFailsWith<IllegalArgumentException> { service.add( invalidSnapshot ) }
        assertFailsWith<IllegalArgumentException> { service.addVersion( invalidSnapshot, "New version" ) }
    }

    @Test
    fun getBy_returns_latest_version_when_no_version_specified() = runSuspendTest {
        val service = createService()
        val protocol = createEmptyProtocol()
        service.add( protocol.getSnapshot(), "In use" )
        modifyProtocol( protocol )
        val lastSnapshot = protocol.getSnapshot()
        service.addVersion( lastSnapshot, "Version 2" )

        val retrieved = service.getBy( protocol.owner, protocol.name )
        assertEquals( lastSnapshot, retrieved )
    }

    @Test
    fun getBy_fails_for_nonexisting_protocol() = runSuspendTest {
        val service = createService()

        val unknownOwner = ProtocolOwner()
        assertFailsWith<IllegalArgumentException> { service.getBy( unknownOwner, "Nope" ) }
    }

    @Test
    fun getAllFor_returns_last_version_of_each_protocol() = runSuspendTest {
        val service = createService()
        val protocol1 = createEmptyProtocol( "Protocol 1" )
        service.add( protocol1.getSnapshot() )
        val protocol2 = createEmptyProtocol( "Protocol 2" )
        service.add( protocol2.getSnapshot() )
        modifyProtocol( protocol2 )
        service.addVersion( protocol2.getSnapshot(), "Version 2" )

        val owner = protocol1.owner // Also owner of protocol2; `createEmptyProtocol` has a fixed owner.
        val protocols = service.getAllFor( owner )
        assertEquals( setOf( protocol1.getSnapshot(), protocol2.getSnapshot() ), protocols.toSet() )
    }

    @Test
    fun getAllFor_returns_empty_list_when_none_found() = runSuspendTest {
        val service = createService()

        val unknown = ProtocolOwner()
        assertTrue( service.getAllFor( unknown ).isEmpty() )
    }

    @Test
    fun getVersionHistoryFor_succeeds() = runSuspendTest {
        val service = createService()
        val protocol = createEmptyProtocol()
        service.add( protocol.getSnapshot(), "1" )
        modifyProtocol( protocol )
        service.addVersion( protocol.getSnapshot(), "2" )

        val history = service.getVersionHistoryFor( protocol.owner, protocol.name )
        assertEquals( setOf( "1", "2" ), history.map { it.tag }.toSet() )
    }

    @Test
    fun getVersionHistoryFor_fails_when_protocol_not_found() = runSuspendTest {
        val service = createService()

        val unknown = ProtocolOwner()
        assertFailsWith<IllegalArgumentException> { service.getVersionHistoryFor( unknown, "Unknown" ) }
    }

    private fun modifyProtocol( protocol: StudyProtocol ): StudyProtocol =
        protocol.apply { addMasterDevice( StubMasterDeviceDescriptor() ) }
}
