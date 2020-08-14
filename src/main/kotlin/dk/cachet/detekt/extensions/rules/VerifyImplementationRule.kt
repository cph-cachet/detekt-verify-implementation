package dk.cachet.detekt.extensions.rules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Rule


/**
 * A rule which verifies whether a concrete class with a specific annotated supertype is implemented correctly.
 */
abstract class VerifyImplementationRule( private val config: Config = Config.empty ): Rule( config )
{
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
     * Retrieve configuration directly from passed config rather than through base class
     * so that it may be used in initializer logic, i.e., to set the issue description.
     */
    private fun <T : Any> prefetchValueOrDefault( ruleId: String, key: String, default: T ): T =
        config.subConfig( ruleId ).valueOrDefault( key, default )
}


const val ANNOTATION_CLASS_CONFIG = "annotationClass"
