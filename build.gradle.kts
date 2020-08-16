import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "dk.cachet.detekt.extensions"
version = "1.0.0-alpha.1"

val jvmTarget = "1.8"
val detektVersion = "1.10.0"
val junit5Version = "5.6.2"
val spek2Version = "2.0.12"


plugins {
    kotlin( "jvm" ) version "1.3.72"
    `maven-publish`
}

repositories {
    jcenter()
}

dependencies {
    implementation( kotlin( "stdlib" ) )
    implementation( "io.gitlab.arturbosch.detekt:detekt-api:$detektVersion" )

    testImplementation( "org.junit.jupiter:junit-jupiter-api:$junit5Version" )
    testRuntimeOnly( "org.junit.jupiter:junit-jupiter-engine:$junit5Version" )
    testImplementation( "org.spekframework.spek2:spek-dsl-jvm:$spek2Version" )
    testRuntimeOnly( "org.spekframework.spek2:spek-runner-junit5:$spek2Version" )

    testImplementation( "io.gitlab.arturbosch.detekt:detekt-parser:$detektVersion" )
    testImplementation( "io.gitlab.arturbosch.detekt:detekt-test:$detektVersion" )
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines( "spek2" )
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = jvmTarget
}

publishing {
    repositories {
        // Publish configuration for GitHub workflows.
        maven {
            name = "GitHubPackages"
            url = uri( "https://maven.pkg.github.com/cph-cachet/detekt-verify-implementation" )
            credentials {
                username = System.getenv( "GITHUB_ACTOR" )
                password = System.getenv( "GITHUB_TOKEN" )
            }
        }
    }
    publications {
        create<MavenPublication>( "default" ) {
            from( components[ "java" ] )

            with ( pom )
            {
                name.set( "Detekt Verify Implementation Plugin" )
                description.set( "A detekt plugin to enable static checking of concrete classes according to annotations on base classes." )
                url.set( "https://github.com/cph-cachet/detekt-verify-implementation" )
                licenses {
                    license {
                        name.set( "MIT License" )
                        url.set( "https://github.com/cph-cachet/detekt-verify-implementation/blob/master/LICENSE.md" )
                    }
                }
                developers {
                    developer {
                        id.set( "whathecode" )
                        name.set( "Steven Jeuris" )
                        email.set( "steven.jeuris@gmail.com" )
                        organization.set( "CACHET" )
                        organizationUrl.set( "http://www.cachet.dk" )
                    }
                }
                scm {
                    connection.set( "scm:git:https://github.com/cph-cachet/detekt-verify-implementation.git" )
                    developerConnection.set( "scm:git:https://github.com/cph-cachet/detekt-verify-implementation.git" )
                    url.set( "https://github.com/cph-cachet/detekt-verify-implementation" )
                }
            }
        }
    }
}