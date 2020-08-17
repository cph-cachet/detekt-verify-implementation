package dk.cachet.detekt.extensions.rules

import io.github.detekt.parser.createKotlinCoreEnvironment
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.junit.jupiter.api.Assertions
import org.spekframework.spek2.Spek


/**
 * Tests for [VerifyImplementationRule].
 */
class VerifyImplementationRuleSpec : Spek({
    test( "fail when annotation does not exist" )
    {
        val config = TestConfig( ANNOTATION_CLASS_CONFIG to "UnknownAnnotation" )
        val rule = ImplementationRuleMock( config )
        val env = createKotlinCoreEnvironment()

        Assertions.assertThrows( IllegalStateException::class.java ) {
            rule.compileAndLintWithContext( env, "" )
        }
    }
})


private class ImplementationRuleMock( config: Config = Config.empty ) : VerifyImplementationRule( config )
{
    override val issue: Issue = Issue( "Mock", Severity.Defect, "Mock for testing", Debt.TWENTY_MINS )
}