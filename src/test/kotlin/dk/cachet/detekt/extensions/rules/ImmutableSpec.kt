package dk.cachet.detekt.extensions.rules

import io.github.detekt.parser.createKotlinCoreEnvironment
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.junit.jupiter.api.Assertions.*
import org.spekframework.spek2.Spek


private const val IMMUTABLE: String = "Immutable"


/**
 * Tests for [Immutable].
 */
class ImmutableSpec : Spek({
    // TODO: Once this behavior (also used in DataClass) is extracted, move these tests to test the common abstraction.
    group( "determine which classes need to be immutable" )
    {
        test( "use fully qualified annotation name" )
        {
            val fullyQualified =
                """
                package some.namespace

                annotation class Immutable

                @Immutable
                class Mutable( var hasMutable: Int )
                """

            val rule = Immutable( "some.namespace.Immutable" )
            val env = createKotlinCoreEnvironment() // Needed for type resolution.

            val hasErrors = rule.compileAndLintWithContext( env, fullyQualified ).isNotEmpty()
            assertFalse( hasErrors )
        }

        test( "only verify annotated classes" )
        {
            val notAnnotated = "class NotImmutable( var invalidMember: String )"
            val isIgnored = isImmutable( notAnnotated )
            assertTrue( isIgnored ) // Even though this class is mutable, the check should not happen.
        }

        test( "verify classes extending from annotated classes" )
        {
            val notAllImmutable =
                """
                @$IMMUTABLE
                abstract class BaseClass
                
                class NotImmutable( var invalidMember: Int = 42 ) : BaseClass()
                """
            assertFalse( isImmutable( notAllImmutable ) )
        }

        test( "verify full inheritance tree" )
        {
            val notAllImmutable =
                """
                @$IMMUTABLE
                abstract class Base
                
                abstract class Intermediate : Base()
                
                class NotImmutable( var invalidMember: Int = 42 ) : Intermediate()
                """
            assertFalse( isImmutable( notAllImmutable ) )
        }

        test( "verify sealed classes" )
        {
            val innerNotImmutable =
                """
                @$IMMUTABLE
                sealed class Outer
                {
                    class Inner( var mutable: Int ) : Outer()
                }
                """
            assertFalse( isImmutable( innerNotImmutable ) )
        }
    }

    test( "verify nullable classes" )
    {
        val immutable = "@$IMMUTABLE class ImmutableClass( val immutable: Int? )"
        assertTrue( isImmutable( immutable ) )

        val mutable =
            """
            class Mutable( var mutable: Int ) 
            
            @$IMMUTABLE
            class( val mutable: Mutable? )
            """
        assertFalse( isImmutable( mutable ) )
    }

    test( "verify used typealias" )
    {
        val immutable =
            """
            class ValidImmutable( val mutable: Int )
            typealias AliasedValidImmutable = ValidImmutable

            @$IMMUTABLE
            class UsesTypealias( val mutable: AliasedValidImmutable )
            """
        assertTrue( isImmutable( immutable ) )
    }

    test( "do not allow type inference" )
    {
        val hasTypeInference =
            """
            @$IMMUTABLE class WithTypeInference { val inferred = 42 }    
            """
        assertFalse( isImmutable( hasTypeInference) )

        val noTypeInference =
            """
            @$IMMUTABLE class WithoutTypeInference { val inferred: Int = 42 }    
            """
        assertTrue( isImmutable( noTypeInference ) )
    }

    test( "constructor properties should be val" )
    {
        val valProperty = "@$IMMUTABLE class ValidImmutable( val validMember: Int = 42 )"
        assertTrue( isImmutable( valProperty ) )

        val varProperty = "@$IMMUTABLE class ValidImmutable( var invalidMember: Int = 42 )"
        assertFalse( isImmutable( varProperty ) )
    }

    test( "constructor properties should be immutable types" )
    {
        val immutableProperty =
            """
            @$IMMUTABLE
            class ImmutableMember( val number: Int = 42 )
            
            @$IMMUTABLE
            class ValidImmutable( val validMember: ImmutableMember )
            """
        assertTrue( isImmutable( immutableProperty ) )

        val mutableProperty =
            """
            class MutableMember( var number: Int = 42 )
            
            @$IMMUTABLE class ValidImmutable( val validMember: MutableMember )
            """
        assertFalse( isImmutable( mutableProperty ) )
    }

    test( "properties should be val" )
    {
        val valProperty = "@$IMMUTABLE class ValidImmutable( val validMember: Int = 42 ) { val validProperty: Int = 42 }"
        assertTrue( isImmutable( valProperty ) )

        val varProperty = "@$IMMUTABLE class NotImmutable( val validMember: Int = 42 ) { var invalidProperty: Int = 42 }"
        assertFalse( isImmutable( varProperty ) )
    }

    test( "properties should be immutable types" )
    {
        val immutableProperty =
            """
            @$IMMUTABLE
            class ImmutableMember( val number: Int = 42 )
            
            @$IMMUTABLE
            class ValidImmutable( val test: Int )
            {
                val validMember: ImmutableMember = ImmutableMember( 42 )
            }
            """
        assertTrue( isImmutable( immutableProperty ) )

        val mutableProperty =
            """
            class MutableMember( var number: Int = 42 )
            
            @$IMMUTABLE
            class ValidImmutable( val test: Int )
            {
                val invalidMember: MutableMember = MutableMember( 42 )
            }
            """
        assertFalse( isImmutable( mutableProperty ) )
    }

    test( "only verify class members" )
    {
        val mutableLocalProperty =
            """
            @$IMMUTABLE
            class Immutable()
            {
                init { var bleh: Int }
            }
            """
        assertTrue( isImmutable( mutableLocalProperty ) )
    }

    test( "report multiple mutable findings" )
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

        val rule = Immutable( "Immutable" )
        val env = createKotlinCoreEnvironment() // Needed for type resolution.

        val errorsReported = rule.compileAndLintWithContext( env, twoMutableMembers ).count()
        assertEquals( 2, errorsReported )
    }

    test( "report types which can't be verified" )
    {
        val unknownType = "@$IMMUTABLE class( val unknown: UnknownType )"
        assertFalse( isImmutable( unknownType ) )
    }
})


private fun isImmutable( code: String ): Boolean
{
    // Add immutable annotation to code.
    val fullCode = code.plus("annotation class $IMMUTABLE" )

    // Evaluate rule for code.
    val rule = Immutable( IMMUTABLE )
    val env = createKotlinCoreEnvironment()
    return rule.compileAndLintWithContext( env, fullCode ).isEmpty()
}
