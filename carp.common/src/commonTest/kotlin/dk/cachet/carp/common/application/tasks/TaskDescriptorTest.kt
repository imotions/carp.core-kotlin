package dk.cachet.carp.common.application.tasks

import dk.cachet.carp.common.application.data.CarpDataTypes
import dk.cachet.carp.common.infrastructure.test.STUB_DATA_TYPE
import dk.cachet.carp.common.infrastructure.test.StubTaskDescriptor
import kotlin.test.*


/**
 * Tests for [TaskDescriptor].
 */
class TaskDescriptorTest
{
    @Test
    fun getAllExpectedDataTypes_succeeds()
    {
        val task = StubTaskDescriptor(
            "Task",
            listOf( Measure.DataStream( STUB_DATA_TYPE ), Measure.TriggerData( 0 ) ),
            "Description"
        )

        val dataTypes = task.getAllExpectedDataTypes()
        val expectedDataTypes = setOf(
            STUB_DATA_TYPE,
            CarpDataTypes.TRIGGERED_TASK.type,
            CarpDataTypes.COMPLETED_TASK.type
        )
        assertEquals( expectedDataTypes, dataTypes )
    }
}
