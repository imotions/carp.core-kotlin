package dk.cachet.carp.common

import dk.cachet.carp.common.serialization.createDefaultJSON
import kotlin.test.*


/**
 * Tests for [MACAddress].
 */
class MACAddressTest
{
    @Test
    fun can_serialize_and_deserialize_MACAddress_using_JSON()
    {
        val mac = MACAddress( "00-11-22-33-44-55" )

        val json = createDefaultJSON()
        val serialized = json.encodeToString( MACAddress.serializer(), mac )
        val parsed = json.decodeFromString( MACAddress.serializer(), serialized )

        assertEquals( mac, parsed )
    }

    @Test
    fun can_initialize_with_alternate_representations()
    {
        val expected = "AA-BB-CC-DD-EE-FF"

        val lowerCase = MACAddress( "aa-bb-cc-dd-ee-ff" )
        assertEquals( expected, lowerCase.address )

        val mixedCase = MACAddress( "aA-bB-cC-dD-eE-fF" )
        assertEquals( expected, mixedCase.address )

        val colonSeparator = MACAddress( "AA:BB:CC:DD:EE:FF" )
        assertEquals( expected, colonSeparator.address )
    }

    @Test
    fun cant_initialize_incorrect_MACAddress()
    {
        assertFailsWith<IllegalArgumentException> { MACAddress( "Invalid" ) }
        // Not long enough.
        assertFailsWith<IllegalArgumentException> { MACAddress( "00-11-22-33-44" ) }
        // Too long.
        assertFailsWith<IllegalArgumentException> { MACAddress( "00-00-00-00-00-00-FF" ) }
        // Invalid character.
        assertFailsWith<IllegalArgumentException> { MACAddress( "G0-00-00-00-00-00" ) }
        // Mixed separators
        assertFailsWith<IllegalArgumentException> { MACAddress( "00-11:22-33:44-55" ) }
        // Unsupported separators.
        assertFailsWith<IllegalArgumentException> { MACAddress( "00 11 22 33 44 55" ) }
    }
}
