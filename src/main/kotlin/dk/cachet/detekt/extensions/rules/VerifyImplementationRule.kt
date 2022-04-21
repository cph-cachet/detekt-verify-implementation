package dk.cachet.detekt.extensions.rules

import dk.cachet.detekt.extensions.psi.TypeResolutionException
import dk.cachet.detekt.extensions.psi.hasAnnotationInHierarchy
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Rule
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe


/**
 * A rule which verifies whether a concrete class with a specific annotated supertype is implemented correctly.
 */
abstract class VerifyImplementationRule( private val config: Config = Config.empty ): Rule( config )
{
    override fun visit( root: KtFile )
    {
        super.visit( root )
        if ( bindingContext == BindingContext.EMPTY ) return

        // Verify whether the configured annotations can be resolved.
        val annotation = valueOrDefault( ANNOTATION_CLASS_CONFIG, "" )
        val canFindAnnotationClass = bindingContext
            .getSliceContents( BindingContext.CLASS )
            .filter { it.value.kind == ClassKind.ANNOTATION_CLASS }
            .map { it.value.fqNameSafe.toString() }
            .contains( annotation )
        if ( !canFindAnnotationClass )
        {
            error( "Can't find configured annotation class `$annotation` for $ruleId in sources." )
        }
    }

    protected fun getFullyQualifiedAnnotationName( ruleId: String ): String =
        prefetchValueOrDefault( ruleId, ANNOTATION_CLASS_CONFIG, "" )

    /**
     * Throw an exception in case the configuration of [VerifyImplementationRule] is not valid.
     */
    protected fun validateConfiguration( ruleId: String )
    {
        val isActive: Boolean = prefetchValueOrDefault( ruleId, Config.ACTIVE_KEY, false )
        val isAnnotationClassSet: Boolean = getFullyQualifiedAnnotationName( ruleId ) != ""

        if ( isActive && !isAnnotationClassSet )
        {
            error( "When $ruleId is active, $ANNOTATION_CLASS_CONFIG needs to be specified." )
        }
    }

    /**
     * Determines whether the specified [classOrObject] has the annotation with [annotationName]
     * applied to any type in its type hierarchy.
     *
     * @throws TypeResolutionException when one of the super types could not be resolved
     * in order to determine whether the annotation is applied to it.
     */
    protected fun hasAnnotationInHierarchy( annotationName: String, classOrObject: KtClassOrObject ): Boolean
    {
        val assumeNoAnnotations: List<String> = valueOrDefault( ASSUME_NO_ANNOTATIONS_CONFIG, emptyList() )
        return classOrObject.hasAnnotationInHierarchy( annotationName, bindingContext, assumeNoAnnotations )
    }

    /**
     * Retrieve configuration directly from passed config rather than through base class
     * so that it may be used in initializer logic, i.e., to set the issue description.
     */
    private fun <T : Any> prefetchValueOrDefault( ruleId: String, key: String, default: T ): T =
        config.subConfig( ruleId ).valueOrDefault( key, default )
}


const val ANNOTATION_CLASS_CONFIG = "annotationClass"
const val ASSUME_NO_ANNOTATIONS_CONFIG = "assumeNoAnnotations"
