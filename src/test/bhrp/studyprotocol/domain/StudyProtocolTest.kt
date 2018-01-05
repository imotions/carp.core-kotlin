package bhrp.studyprotocol.domain

import bhrp.studyprotocol.domain.deployment.*
import bhrp.studyprotocol.domain.devices.*
import bhrp.studyprotocol.domain.tasks.*
import bhrp.studyprotocol.domain.triggers.*
import org.junit.jupiter.api.*
import kotlin.test.*


/**
 * Tests for [StudyProtocol].
 */
class StudyProtocolTest : DeviceConfigurationTest, TaskConfigurationTest
{
    override fun createDeviceConfiguration(): DeviceConfiguration
    {
        return createEmptyProtocol()
    }

    override fun createTaskConfiguration(): TaskConfiguration
    {
        return createEmptyProtocol()
    }


    @Test
    fun `one master device needed for deployment`()
    {
        // By default, no master device is defined in a study protocol.
        val protocol: StudyProtocol = createEmptyProtocol()

        // Therefore, the protocol is not deployable, indicated by an error in deployment issues.
        assertFalse( protocol.isDeployable() )
        assertEquals( 1, protocol.getDeploymentIssues().filter { it is NoMasterDeviceError }.count() )
    }


    @Test
    fun `addTrigger succeeds`()
    {
        val protocol = createEmptyProtocol()
        val device = StubMasterDeviceDescriptor()
        protocol.addMasterDevice( device )
        val trigger = StubTrigger( device )

        val isAdded: Boolean = protocol.addTrigger( trigger )
        assertTrue( isAdded )
        assertTrue( protocol.triggers.contains( trigger ) )
    }

    @Test
    fun `addTrigger multiple times only adds first time`()
    {
        val protocol = createEmptyProtocol()
        val device = StubMasterDeviceDescriptor()
        protocol.addMasterDevice( device )
        val trigger = StubTrigger( device )
        protocol.addTrigger( trigger )

        val isAdded: Boolean = protocol.addTrigger( trigger )
        assertFalse( isAdded )
        assertEquals( 1, protocol.triggers.count() )
    }

    @Test
    fun `can't addTrigger for device not included in the protocol`()
    {
        val protocol = createEmptyProtocol()
        val trigger = StubTrigger( StubDeviceDescriptor() )

        assertFailsWith<InvalidConfigurationError>
        {
            protocol.addTrigger( trigger )
        }
    }

    @Test
    fun `can't add trigger for task not included in the protocol`()
    {
        fail( "Triggers are not implemented yet." )
    }

    @Test
    fun `triggers should not send more than one task to a single device`()
    {
        fail( "Triggers are not implemented yet." )
    }

    @Test
    fun `deployment warning when some devices never receive tasks`()
    {
        fail( "To implement" )
    }

    @Test
    fun `removing a task also removes it from triggers`()
    {
        // Create a study protocol with a task which is initiated by a trigger.
        val protocol = createEmptyProtocol()
        val task = StubTaskDescriptor()
        protocol.addTask( task )
        // TODO: Add task to trigger.

        // TODO: Remove task and check for removal from trigger.

        fail( "Triggers are not implemented yet." )
    }

    @Test
    fun `deployment warning when some tasks are never triggered`()
    {
        // Create a study protocol with a task which is never triggered.
        val protocol = createEmptyProtocol()
        protocol.addTask( StubTaskDescriptor() )

        // Therefore, a warning is issued.
        assertEquals( 1, protocol.getDeploymentIssues().filter { it is UntriggeredTasksWarning }.count() )
    }
}