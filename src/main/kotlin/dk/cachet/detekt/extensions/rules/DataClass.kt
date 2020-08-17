package dk.cachet.detekt.extensions.rules

import dk.cachet.detekt.extensions.psi.TypeResolutionException
import dk.cachet.detekt.extensions.psi.hasAnnotationInHierarchy
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isAbstract


class DataClass( config: Config = Config.empty )
    : VerifyImplementationRule( config )
{
    private val id = javaClass.simpleName
    private val annotationName: String = getFullyQualifiedAnnotationName( id )
    init { validateConfiguration( id ) }

    override val issue: Issue = Issue(
        id,
        Severity.Defect,
        "Classes extending from types with @$annotationName applied to them should be data classes.",
        Debt.TWENTY_MINS
    )

    override fun visitClassOrObject( classOrObject: KtClassOrObject )
    {
        val shouldBeDataClass =
            try { classOrObject.hasAnnotationInHierarchy( annotationName, bindingContext ) }
            catch ( ex: TypeResolutionException )
            {
                val cantAnalyze = Issue( issue.id, Severity.Warning, issue.description, Debt.FIVE_MINS )
                val message = "Cannot verify whether base class `${ex.typeName}` should be a data class since the source is unavailable."
                report( CodeSmell( cantAnalyze, Entity.from( classOrObject ), message ) )

                false
            }

        if ( shouldBeDataClass )
        {
            val klass = classOrObject as? KtClass
            if ( klass != null )
            {
                val isAbstract = klass.isAbstract() || klass.isSealed()
                if ( !isAbstract && !klass.isData() )
                {
                    val message = "`${classOrObject.name}` should be a data class."
                    report( CodeSmell( issue, Entity.from( classOrObject ), message ) )
                }
            }
        }

        super.visitClassOrObject( classOrObject )
    }
}
