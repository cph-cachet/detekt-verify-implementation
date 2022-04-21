package dk.cachet.detekt.extensions.rules

import dk.cachet.detekt.extensions.psi.TypeResolutionException
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.internal.RequiresTypeResolution
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.isAbstract
import org.jetbrains.kotlin.resolve.BindingContext


/**
 * A rule which requires extending classes from types to which a configured annotation has been applied
 * to be data classes or object declarations.
 * This guarantees a predictable default implementation for `equals` and `hashcode` implementations, i.e.,
 * value equality instead of referential equality.
 */
@RequiresTypeResolution
class DataClass( config: Config = Config.empty )
    : VerifyImplementationRule( config )
{
    private val id = javaClass.simpleName
    private val annotationName: String = getFullyQualifiedAnnotationName( id )
    init { validateConfiguration( id ) }

    override val issue: Issue = Issue(
        id,
        Severity.Defect,
        "Classes extending from types with @$annotationName applied to them should be data classes or object declarations.",
        Debt.TWENTY_MINS
    )

    override fun visitClassOrObject( classOrObject: KtClassOrObject )
    {
        super.visitClassOrObject( classOrObject )
        if ( bindingContext == BindingContext.EMPTY ) return

        val shouldBeDataClass =
            try { hasAnnotationInHierarchy( annotationName, classOrObject ) }
            catch ( ex: TypeResolutionException )
            {
                val cantAnalyze = Issue( issue.id, Severity.Warning, issue.description, Debt.FIVE_MINS )
                val message =
                    "Cannot verify whether base class `${ex.typeName}` requires extending classes " +
                    "to be a data classes or object declarations since the source is unavailable."
                report( CodeSmell( cantAnalyze, Entity.from( classOrObject ), message ) )

                false
            }

        val isObjectDeclaration = classOrObject is KtObjectDeclaration
        if ( shouldBeDataClass && !isObjectDeclaration )
        {
            val klass = classOrObject as? KtClass
            if ( klass != null )
            {
                val isAbstract = klass.isAbstract() || klass.isSealed()
                if ( !isAbstract && !klass.isData() )
                {
                    val message = "`${classOrObject.name}` should be a data class or object declaration."
                    report( CodeSmell( issue, Entity.from( classOrObject ), message ) )
                }
            }
        }
    }
}
