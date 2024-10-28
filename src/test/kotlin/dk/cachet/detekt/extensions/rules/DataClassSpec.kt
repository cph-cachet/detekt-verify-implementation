package dk.cachet.detekt.extensions.rules

import io.github.detekt.parser.createKotlinCoreEnvironment
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Assertions.*
import org.spekframework.spek2.Spek
import java.lang.IllegalStateException


private const val ANNOTATION: String = "DataClass"
private const val BASE: String = "Base"


/**
 * Tests for [DataClass].
 */
class DataClassSpec : Spek({
    test( "when rule is active $ANNOTATION_CLASS_CONFIG should be set" )
    {
        assertThrows( IllegalStateException::class.java )
        {
            DataClass( TestConfig( Config.ACTIVE_KEY to true ) )
        }
    }

    test( "concrete classes should be data classes" )
    {
        val dataClass = "data class IsDataClass( val member: Int = 42 )"
        assertRulePasses( dataClass )

        val noDataClass = "class NoDataClass( val member: Int = 42 )"
        assertRuleFails( noDataClass )
    }

    test( "abstract classes don't need to be data classes" )
    {
        val abstractClass = "abstract class Extending"
        assertRulePasses( abstractClass )
    }

    test( "sealed classes don't need to be data classes" )
    {
        val sealedClass = "sealed class Sealed"
        assertRulePasses( sealedClass )
    }

    test( "objects don't have to be data classes" )
    {
        val objectClass = "object SomeObject"
        assertRulePasses( objectClass )
    }
})


private fun assertRulePasses( code: String )
{
    val withAbstractBase = code.addAbstractBase()
    assertTrue( getFindings( withAbstractBase ).isEmpty() )

    val withInterfaceBase = code.addInterfaceBase()
    assertTrue( getFindings( withInterfaceBase ).isEmpty() )
}

private fun assertRuleFails( code: String )
{
    val withAbstractBase = code.addAbstractBase()
    assertFalse( getFindings( withAbstractBase ).isEmpty() )

    val withInterfaceBase = code.addInterfaceBase()
    assertFalse( getFindings( withInterfaceBase ).isEmpty() )
}

private fun String.addAbstractBase() = this.plus(
    """ : $BASE()
    annotation class $ANNOTATION
    @$ANNOTATION abstract class $BASE
    """
)

private fun String.addInterfaceBase() = this.plus(
    """ : $BASE
    annotation class $ANNOTATION
    @$ANNOTATION interface $BASE
    """
)

private fun getFindings( code: String ): List<Finding>
{
    val config = TestConfig( ANNOTATION_CLASS_CONFIG to ANNOTATION )
    val rule = DataClass( config )
    val env: KotlinCoreEnvironment = // Needed for type resolution.
        createKotlinCoreEnvironment( printStream = System.err )
    return rule.compileAndLintWithContext( env, code )
}
