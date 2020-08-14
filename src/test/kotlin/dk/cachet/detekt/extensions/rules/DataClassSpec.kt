package dk.cachet.detekt.extensions.rules

import io.github.detekt.parser.createKotlinCoreEnvironment
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.junit.jupiter.api.Assertions.*
import org.spekframework.spek2.Spek


private const val BASE: String = "Base"


/**
 * Tests for [DataClass].
 */
class DataClassSpec : Spek({
    test( "concrete classes should be data classes" )
    {
        val dataClass = "data class IsDataClass( val member: Int = 42 ) : $BASE()"
        assertTrue( isDataClass( dataClass ) )

        val noDataClass = "class NoDataClass( val member: Int = 42 ) : $BASE()"
        assertFalse( isDataClass( noDataClass ) )
    }

    test( "abstract classes don't need to be data classes" )
    {
        val abstractClass = "abstract class Extending : $BASE()"
        assertTrue( isDataClass( abstractClass ) )
    }

    test( "sealed classes don't need to be data classes" )
    {
        val sealedClass = "sealed class Sealed : $BASE()"
        assertTrue( isDataClass( sealedClass ) )
    }
})


private fun isDataClass( code: String ): Boolean
{
    // Add annotated base class to code.
    val annotation = "DataClass"
    val fullCode = code.plus(
        """
        annotation class $annotation
        @DataClass abstract class $BASE
        """ )

    // Evaluate rule for code.
    val config = TestConfig( ANNOTATION_CLASS_CONFIG to annotation )
    val rule = DataClass( config )
    val env = createKotlinCoreEnvironment() // Needed for type resolution.
    val findings = rule.compileAndLintWithContext( env, fullCode )
    return findings.isEmpty()
}
