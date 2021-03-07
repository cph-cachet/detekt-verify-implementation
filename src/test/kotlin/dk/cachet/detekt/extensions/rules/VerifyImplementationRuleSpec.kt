package dk.cachet.detekt.extensions.rules

import dk.cachet.detekt.extensions.psi.TypeResolutionException
import io.github.detekt.parser.createKotlinCoreEnvironment
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.junit.jupiter.api.Assertions.*
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

        assertThrows( IllegalStateException::class.java ) {
            rule.compileAndLintWithContext( env, "" )
        }
    }

    test( "hasAnnotationInHierarchy fails when base class can't be verified" )
    {
        val config = TestConfig( ANNOTATION_CLASS_CONFIG to ImplementationRuleMock.ANNOTATION_NAME )
        val rule = ImplementationRuleMock( config )
        val env = createKotlinCoreEnvironment()

        val code =
            """
            annotation class Annotation
            class SomeClass : Any()
            """
        assertThrows( TypeResolutionException::class.java ) {
            rule.compileAndLintWithContext( env, code )
        }
    }

    test( "hasAnnotationInHierarchy does not analyze classes set in $ASSUME_NO_ANNOTATIONS_CONFIG" )
    {
        val config = TestConfig(
            ANNOTATION_CLASS_CONFIG to ImplementationRuleMock.ANNOTATION_NAME,
            ASSUME_NO_ANNOTATIONS_CONFIG to listOf( "kotlin.Any" ) // Any is in stdlib and can't be resolved.
        )
        val rule = ImplementationRuleMock( config )
        val env = createKotlinCoreEnvironment()

        val code =
            """
            annotation class Annotation
            class SomeClass : Any()
            """
        val errorsReported = rule.compileAndLintWithContext( env, code ).count()
        assertEquals( 0, errorsReported )
    }
})


private class ImplementationRuleMock( config: Config = Config.empty ) : VerifyImplementationRule( config )
{
    companion object
    {
        const val ANNOTATION_NAME = "Annotation"
    }

    override val issue: Issue = Issue( "Mock", Severity.Defect, "Mock for testing", Debt.TWENTY_MINS )

    override fun visitClassOrObject( classOrObject: KtClassOrObject)
    {
        // This is called by typical implementations, and its behavior is tested here.
        hasAnnotationInHierarchy( ANNOTATION_NAME, classOrObject )

        super.visitClassOrObject( classOrObject )
    }
}