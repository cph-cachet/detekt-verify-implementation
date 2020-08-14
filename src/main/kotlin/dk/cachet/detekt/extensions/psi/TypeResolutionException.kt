package dk.cachet.detekt.extensions.psi


/**
 * An exception which is thrown when a given [typeName] cannot be resolved.
 */
class TypeResolutionException( val typeName: String ) : Throwable()
