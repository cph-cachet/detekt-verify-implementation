package dk.cachet.detekt.extensions.rules

import io.github.detekt.parser.createKotlinCoreEnvironment
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.junit.jupiter.api.Assertions.*
import org.spekframework.spek2.Spek


private const val IMMUTABLE: String = "Immutable"


/**
 * Tests for [Immutable].
 */
class ImmutableSpec : Spek({
    test( "when rule is active $ANNOTATION_CLASS_CONFIG should be set" )
    {
        assertThrows( IllegalStateException::class.java )
        {
            Immutable( TestConfig( Config.ACTIVE_KEY to true ) )
        }
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

    test( "verify nullable constructor properties" )
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

    test( "verify nullable properties" )
    {
        val immutable = "@$IMMUTABLE class ImmutableClass { val defaultNull: Int? = null }"
        assertTrue( isImmutable( immutable ) )
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

    test( "do not allow generic class members" )
    {
        val hasGenericClassMember =
            """
            @$IMMUTABLE class WithGeneric<T>( val member: T )
            """.trimIndent()

        assertFalse( isImmutable( hasGenericClassMember ) )
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

        val config = TestConfig( ANNOTATION_CLASS_CONFIG to "Immutable" )
        val rule = Immutable( config )
        val env = createKotlinCoreEnvironment() // Needed for type resolution.

        val errorsReported = rule.compileAndLintWithContext( env, twoMutableMembers ).count()
        assertEquals( 2, errorsReported )
    }

    test( "report types which can't be verified" )
    {
        val unknownType = "@$IMMUTABLE class SomeClass( val unknown: UnknownType )"
        assertFalse( isImmutable( unknownType ) )
    }
})


private fun isImmutable( code: String ): Boolean
{
    // Add immutable annotation to code.
    val fullCode = code.plus( "annotation class $IMMUTABLE" )

    // Evaluate rule for code.
    val config = TestConfig( ANNOTATION_CLASS_CONFIG to IMMUTABLE )
    val rule = Immutable( config )
    val env = createKotlinCoreEnvironment()
    val findings = rule.compileAndLintWithContext( env, fullCode )

    return findings.isEmpty()
}
