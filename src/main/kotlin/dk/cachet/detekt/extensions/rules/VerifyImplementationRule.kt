package dk.cachet.detekt.extensions.rules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Rule


/**
 * A rule which verifies whether a concrete class with a specific annotated supertype is implemented correctly.
 */
abstract class VerifyImplementationRule( private val config: Config = Config.empty ): Rule( config )
{
    // This is retrieved directly from config rather than through the base class
    // so that it may be used in initializer logic, i.e., to set the issue description.
    protected fun getFullyQualifiedAnnotationName( ruleId: String ): String =
        config.subConfig( ruleId ).valueOrDefault( ANNOTATION_CLASS_CONFIG, "" )
}


const val ANNOTATION_CLASS_CONFIG = "annotationClass"
