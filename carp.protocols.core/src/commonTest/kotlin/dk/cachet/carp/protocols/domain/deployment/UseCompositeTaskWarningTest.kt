package dk.cachet.carp.protocols.domain.deployment

import dk.cachet.carp.common.application.triggers.TaskControl
import dk.cachet.carp.common.infrastructure.test.StubDeviceDescriptor
import dk.cachet.carp.common.infrastructure.test.StubMasterDeviceDescriptor
import dk.cachet.carp.common.infrastructure.test.StubTaskDescriptor
import dk.cachet.carp.common.infrastructure.test.StubTrigger
import dk.cachet.carp.protocols.domain.start
import dk.cachet.carp.protocols.infrastructure.test.createEmptyProtocol
import kotlin.test.*


/**
 * Tests for [UseCompositeTaskWarning].
 */
class UseCompositeTaskWarningTest
{
    @Test
    fun isIssuePresent_true_when_multiple_tasks_are_sent_to_one_device_by_one_trigger()
    {
        val protocol = createEmptyProtocol()
        val device = StubMasterDeviceDescriptor()
        val trigger = StubTrigger( device )
        with ( protocol )
        {
            addMasterDevice( device )
            addTaskControl( trigger, StubTaskDescriptor( "Task 1" ), device, TaskControl.Control.Start )
            addTaskControl( trigger, StubTaskDescriptor( "Task 2" ), device, TaskControl.Control.Start )
        }

        val warning = UseCompositeTaskWarning()
        assertTrue( warning.isIssuePresent( protocol ) )
    }

    @Test
    fun isIssuePresent_false_when_only_single_tasks_are_triggered_per_device()
    {
        val protocol = createEmptyProtocol()
        val device1 = StubMasterDeviceDescriptor()
        val device2 = StubDeviceDescriptor()
        val task = StubTaskDescriptor()
        with ( protocol )
        {
            addMasterDevice( device1 )
            addConnectedDevice( device2, device1 )
            val trigger1 = StubTrigger( device1 )
            addTaskControl( trigger1.start( task, device1 ) )
            addTaskControl( trigger1.start( task, device2 ) )
            addTaskControl( StubTrigger( device2 ).start( task, device1 ) )
        }

        val warning = UseCompositeTaskWarning()
        assertFalse( warning.isIssuePresent( protocol ) )
    }

    @Test
    fun getOverlappingTasks_returns_all_overlapping_tasks()
    {
        val protocol = createEmptyProtocol()
        val device = StubMasterDeviceDescriptor()
        val trigger = StubTrigger( device )
        val task1 = StubTaskDescriptor( "Task 1" )
        val task2 = StubTaskDescriptor( "Task 2" )
        with ( protocol )
        {
            addMasterDevice( device )
            addTaskControl( trigger.start( task1, device ) )
            addTaskControl( trigger.start( task2, device ) )
        }

        val warning = UseCompositeTaskWarning()
        val overlapping = warning.getOverlappingTasks( protocol )
        val expectedOverlapping = listOf( UseCompositeTaskWarning.OverlappingTasks( trigger, device, listOf( task1, task2 ) ) )
        assertEquals( expectedOverlapping.count(), overlapping.intersect( expectedOverlapping ).count() )
    }
}
