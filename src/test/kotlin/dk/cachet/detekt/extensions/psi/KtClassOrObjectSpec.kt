package dk.cachet.detekt.extensions.psi

import io.github.detekt.parser.createKotlinCoreEnvironment
import io.github.detekt.test.utils.compileContentForTest
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.junit.jupiter.api.Assertions.*
import org.spekframework.spek2.Spek


class KtClassOrObjectSpec : Spek({
    group( "hasAnnotationInHierarchy" )
    {
        test("hasAnnotationInHierarchy finds fully qualified annotation name on class" )
        {
            val code =
                """
                package some

                annotation class Annotation

                @Annotation
                class Annotated
                """
            val classOrObject = compileAndFindClass( code, "Annotated" )

            val hasAnnotation = classOrObject.hasAnnotationInHierarchy( "some.Annotation" )
            assertTrue( hasAnnotation )
        }

        test("hasAnnotationInHierarchy finds fully qualified annotation name on interface" )
        {
            val code =
                """
                package some

                annotation class Annotation

                @Annotation
                interface Annotated
                """
            val classOrObject = compileAndFindClass( code, "Annotated" )

            val hasAnnotation = classOrObject.hasAnnotationInHierarchy( "some.Annotation" )
            assertTrue( hasAnnotation )
        }

        test( "hasAnnotationInHierarchy is false when annotation is not present" )
        {
            val code = "class Unannotated"
            val classOrObject = compileAndFindClass( code, "Unannotated" )

            val hasAnnotation = classOrObject.hasAnnotationInHierarchy( "Annotation" )
            assertFalse( hasAnnotation )
        }

        test( "hasAnnotationInHierarchy finds annotation on base class" )
        {
            val code =
                """
                annotation class Annotation
                
                @Annotation
                abstract class BaseClass
                
                class ExtendingClass : BaseClass()
                """
            val classOrObject = compileAndFindClass( code, "ExtendingClass" )

            val hasAnnotation = classOrObject.hasAnnotationInHierarchy( "Annotation" )
            assertTrue( hasAnnotation )
        }

        test( "hasAnnotationInHierarchy finds annotation on base interface" )
        {
            val code =
                """
                annotation class Annotation
                
                @Annotation
                interface Base
                
                class ExtendingClass : Base
                """
            val classOrObject = compileAndFindClass( code, "ExtendingClass" )

            val hasAnnotation = classOrObject.hasAnnotationInHierarchy( "Annotation" )
            assertTrue( hasAnnotation )
        }

        test( "hasAnnotationInHierarchy finds annotation deeper in hierarchy" )
        {
            val code =
                """
                annotation class Annotation
                
                abstract class Base
                abstract class Intermediate : Base()
                
                @Annotation
                class ExtendingClass : Intermediate()
                """
            val classOrObject = compileAndFindClass( code, "ExtendingClass" )

            val hasAnnotation = classOrObject.hasAnnotationInHierarchy( "Annotation" )
            assertTrue( hasAnnotation )
        }

        test( "hasAnnotationInHierarchy fails for types which can't be resolved" )
        {
            val code =
                """
                annotation class Annotation
                 
                class Annotated : Any() // Any is in stdlib and can't be resolved.
                """
            val classOrObject = compileAndFindClass( code, "Annotated" )
            assertThrows( TypeResolutionException::class.java ) {
                classOrObject.hasAnnotationInHierarchy( "Annotation" )
            }
        }

        test( "hasAnnotationInHierarchy ignores assumeHasNoAnnotation type names" )
        {
            val code =
                """
                annotation class Annotation
                 
                class Annotated : Any()
                """
            val classOrObject = compileAndFindClass( code, "Annotated" )

            val hasAnnotation =
                classOrObject.hasAnnotationInHierarchy( "Annotation", listOf( "kotlin.Any" ) )
            assertFalse( hasAnnotation )
        }

        test( "hasAnnotationInHierarchy ignores assumeHasAnnotation type names recursively" )
        {
            val code =
                """
                annotation class Annotation
                 
                class Base : Any()
                class Annotated : Base()
                """
            val classOrObject = compileAndFindClass( code, "Annotated" )

            val hasAnnotation =
                classOrObject.hasAnnotationInHierarchy( "Annotation", listOf( "kotlin.Any" ) )
            assertFalse( hasAnnotation )
        }
    }
})


private data class CompiledClassOrObject(
    private val classOrObject: KtClassOrObject,
    private val bindingContext: BindingContext )
{
    fun hasAnnotationInHierarchy(
        fullyQualifiedAnnotationName: String,
        assumeHasNoAnnotation: List<String> = emptyList()
    ): Boolean =
        classOrObject.hasAnnotationInHierarchy( fullyQualifiedAnnotationName, bindingContext, assumeHasNoAnnotation )
}


private fun compileAndFindClass( code: String, name: String ): CompiledClassOrObject
{
    val file: KtFile = compileContentForTest( code )
    val classOrObject: KtClassOrObject =
        file.children.filterIsInstance<KtClassOrObject>().first { it.name == name }

    val env: KotlinCoreEnvironment = // Needed for type resolution.
        createKotlinCoreEnvironment( printStream = System.err )
    val bindingContext: BindingContext = getContextForPaths( env, listOf( file ) )

    return CompiledClassOrObject( classOrObject, bindingContext )
}

private fun getContextForPaths( environment: KotlinCoreEnvironment, paths: List<KtFile> ) =
    TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
        environment.project,
        paths,
        NoScopeRecordCliBindingTrace( environment.project ),
        environment.configuration,
        environment::createPackagePartProvider,
        ::FileBasedDeclarationProviderFactory
    ).bindingContext
