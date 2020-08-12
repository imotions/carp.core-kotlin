package dk.cachet.carp.detekt.extensions.rules

import io.gitlab.arturbosch.detekt.test.KtTestCompiler
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import kotlin.test.*


/**
 * Tests for [VerifyImmutable].
 */
class VerifyImmutableTest
{
    @Test
    fun use_fully_qualified_annotation_name()
    {
        val fullyQualified =
            """
            package some.namespace
            
            annotation class Immutable
            
            @Immutable class Mutable( var hasMutable: Int )
            """

        val rule = VerifyImmutable( "some.namespace.Immutable" )
        val env = KtTestCompiler.createEnvironment().env

        val errorsReported = rule.compileAndLintWithContext( env, fullyQualified ).isNotEmpty()
        assertTrue( errorsReported )
    }

    @Test
    fun only_verify_annotated_classes()
    {
        val notAnnotated = "class NotImmutable( var invalidMember: String )"
        val isIgnored = isImmutable( notAnnotated )
        assertTrue( isIgnored ) // Even though this class is mutable, the check should not happen.
    }

    @Test
    fun verify_classes_extending_from_annotated_classes()
    {
        val notAllImmutable =
            """
            @Immutable
            abstract class BaseClass
             
            class NotImmutable( var invalidMember: Int = 42 ) : BaseClass()
            """
        assertFalse( isImmutable( notAllImmutable ) )
    }

    @Test
    fun verify_full_inheritance_tree()
    {
        val notAllImmutable =
            """
            annotation class Immutable
            
            @Immutable
            abstract class LastBase
            
            abstract class BaseClass : LastBase()
             
            class NotImmutable( var invalidMember: Int = 42 ) : BaseClass()
            """
        assertFalse( isImmutable( notAllImmutable ) )
    }

    @Test
    fun verify_sealed_classes()
    {
        val innerNotImmutable =
            """
            @Immutable
            sealed class Outer
            {
                class Inner( var mutable: Int ) : Outer()
            }
            """
        assertFalse( isImmutable( innerNotImmutable ) )
    }

    @Test
    fun verify_nullable_classes()
    {
        val immutable = "@Immutable class ImmutableClass( val immutable: Int? )"
        assertTrue( isImmutable( immutable ) )

        val mutable =
            """
            class Mutable( var mutable: Int ) 
            @Immutable class( val mutable: Mutable? )
            """
        assertFalse( isImmutable( mutable ) )
    }

    @Test
    fun verify_used_typealias()
    {
        val immutable =
            """
            class ValidImmutable( val mutable: Int )
            typealias AliasedValidImmutable = ValidImmutable
            
            @Immutable
            class UsesTypealias( val mutable: AliasedValidImmutable ) 
            """
        assertTrue( isImmutable( immutable ) )
    }

    @Test
    fun do_not_allow_type_inference()
    {
        val hasTypeInference =
            """
            @Immutable class WithTypeInference { val inferred = 42 }    
            """
        assertFalse( isImmutable( hasTypeInference) )

        val noTypeInference =
            """
            @Immutable class WithoutTypeInference { val inferred: Int = 42 }    
            """
        assertTrue( isImmutable( noTypeInference ) )
    }

    @Test
    fun constructor_properties_should_be_val()
    {
        val valProperty = "@Immutable class ValidImmutable( val validMember: Int = 42 )"
        assertTrue( isImmutable( valProperty ) )

        val varProperty = "@Immutable class ValidImmutable( var invalidMember: Int = 42 )"
        assertFalse( isImmutable( varProperty ) )
    }

    @Test
    fun constructor_properties_should_be_immutable_types()
    {
        val immutableProperty =
            """
            @Immutable class ImmutableMember( val number: Int = 42 )
            @Immutable class ValidImmutable( val validMember: ImmutableMember )
            """
        assertTrue( isImmutable( immutableProperty ) )

        val mutableProperty =
            """
            class MutableMember( var number: Int = 42 )
            @Immutable class ValidImmutable( val validMember: MutableMember )
            """
        assertFalse( isImmutable( mutableProperty ) )
    }

    @Test
    fun properties_should_be_val()
    {
        val valProperty = "@Immutable class ValidImmutable( val validMember: Int = 42 ) { val validProperty: Int = 42 } "
        assertTrue( isImmutable( valProperty ) )

        val varProperty = "@Immutable class NotImmutable( val validMember: Int = 42 ) { var invalidProperty: Int = 42 }"
        assertFalse( isImmutable( varProperty ) )
    }

    @Test
    fun properties_should_be_immutable_types()
    {
        val immutableProperty =
            """
            @Immutable class ImmutableMember( val number: Int = 42 )
            @Immutable class ValidImmutable( val test: Int )
            {
                val validMember: ImmutableMember = ImmutableMember( 42 )
            }
            """
        assertTrue( isImmutable( immutableProperty ) )

        val mutableProperty =
            """
            class MutableMember( var number: Int = 42 )
            @Immutable class ValidImmutable( val test: Int )
            {
                val invalidMember: MutableMember = MutableMember( 42 )
            }
            """
        assertFalse( isImmutable( mutableProperty ) )
    }

    @Test
    fun only_verify_class_members()
    {
        val mutableLocalProperty =
            """
            @Immutable class Immutable()
            {
                init { var bleh: Int }
            }
            """
        assertTrue( isImmutable( mutableLocalProperty ) )
    }

    @Test
    fun report_multiple_mutable_findings()
    {
        val twoMutableMembers =
            """
            annotation class Immutable
            @Immutable class NotImmutable( val immutable: Int )
            {
                var one: Int = 42
                var two: Int = 42
            }
            """
        val rule = VerifyImmutable( "Immutable" )
        val env = KtTestCompiler.createEnvironment().env

        assertEquals( 2, rule.compileAndLintWithContext( env, twoMutableMembers ).count() )
    }

    @Test
    fun report_types_which_cant_be_verified()
    {
        val unknownType = "@Immutable class( val unknown: UnknownType )"
        assertFalse( isImmutable( unknownType ) )
    }

    private fun isImmutable( code: String ): Boolean
    {
        // Add immutable annotation.
        val fullCode = code.plus( "annotation class Immutable" )

        val rule = VerifyImmutable( "Immutable" )
        val env = KtTestCompiler.createEnvironment().env

        return rule.compileAndLintWithContext( env, fullCode ).isEmpty()
    }
}