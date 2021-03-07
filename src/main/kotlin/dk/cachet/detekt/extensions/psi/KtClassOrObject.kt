package dk.cachet.detekt.extensions.psi

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.source.getPsi


/**
 * Determines whether a type or any of its super types have a specific annotation applied to it.
 *
 * @throws TypeResolutionException when one of the super types needed to be resolved but couldn't
 * and thus the annotations could not be analyzed.
 */
fun KtClassOrObject.hasAnnotationInHierarchy(
    fullyQualifiedAnnotationName: String,
    bindingContext: BindingContext,
    /**
     * A list of fully qualified type names for which it is assumed they don't have the annotation applied to them.
     * When encountering a type with the given type name, no type resolution will be attempted.
     */
    assumeHasNoAnnotation: List<String> = emptyList()
): Boolean
{
    // Verify whether the annotation is applied to this type.
    val hasAnnotation = this.annotationEntries
        .any {
            val annotationType = it.typeReference?.typeElement as KtUserType
            val annotationName: String? = annotationType.referenceExpression
                ?.getReferenceTargets( bindingContext )
                ?.filterIsInstance<ClassConstructorDescriptor>()?.firstOrNull()
                ?.constructedClass?.fqNameSafe?.asString()
            annotationName == fullyQualifiedAnnotationName
        }
    if ( hasAnnotation ) return true

    // Verify whether any of the super types has the annotation applied.
    val superTypes = this.superTypeListEntries
        .map { it.typeAsUserType?.referenceExpression?.getReferenceTargets( bindingContext )?.singleOrNull() }
    val anyBaseClassWithAnnotation = superTypes
        .filterIsInstance<ClassConstructorDescriptor>()
        .filter { !assumeHasNoAnnotation.contains( it.constructedClass.fqNameSafe.toString() ) }
        .map {
            it.constructedClass.source.getPsi() as KtClassOrObject?
                ?: throw TypeResolutionException( it.constructedClass.name.toString() )
        }
        .any { it.hasAnnotationInHierarchy( fullyQualifiedAnnotationName, bindingContext, assumeHasNoAnnotation ) }
    val anyInterfaceWithAnnotation = superTypes
        .filterIsInstance<ClassDescriptor>()
        .filter {
            it.kind == ClassKind.INTERFACE &&
            !assumeHasNoAnnotation.contains( it.fqNameSafe.toString() )
        }
        .map {
            it.source.getPsi() as KtClassOrObject?
                ?: throw TypeResolutionException( it.name.toString() )
        }
        .any { it.hasAnnotationInHierarchy( fullyQualifiedAnnotationName, bindingContext, assumeHasNoAnnotation ) }

    return anyBaseClassWithAnnotation || anyInterfaceWithAnnotation
}
