import java.io.File
import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "dk.cachet.detekt.extensions"
version = "1.2.2"

val jvmTarget = "1.8"
val detektVersion = "1.17.0"
val junit5Version = "5.7.2"
val spek2Version = "2.0.15"


plugins {
    kotlin( "jvm" ) version "1.5.0"
    id( "org.jetbrains.dokka" ) version "1.4.32"
    `maven-publish`
    signing
    id( "io.codearte.nexus-staging" ) version "0.30.0"
}

repositories {
    mavenCentral()
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

tasks {
    withType<Test> {
        useJUnitPlatform {
            includeEngines( "spek2" )
        }
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = jvmTarget
    }

    dokkaHtml {
        outputDirectory.set(buildDir.resolve("dokka"))
    }
}
val sourcesJar by tasks.creating( Jar::class )
{
    archiveClassifier.set( "sources" )
    from( sourceSets.getByName( "main" ).allSource )
}
val javadocJar by tasks.creating( Jar::class )
{
    archiveClassifier.set( "javadoc" )
    from( tasks.dokkaJavadoc )
}

// Publish configuration.
// For signing and publishing to work, a 'publish.properties' file needs to be added to the root containing:
// The OpenPGP credentials to sign all artifacts:
// > signing.keyFile=<ABSOLUTE PATH TO THE ASCII-ARMORED KEY FILE>
// > signing.password=<SECRET>
// A username and password to upload artifacts to the Sonatype repository:
// > repository.username=<SONATYPE USERNAME>
// > repository.password=<SONATYPE PASSWORD>
val publishProperties = Properties()
val publishPropertiesFile = file( "publish.properties" )
if ( publishPropertiesFile.exists() )
{
    publishProperties.load( FileInputStream( publishPropertiesFile ) )
}
val nexusUsername: String = publishProperties.getProperty( "repository.username", "" )
val nexusPassword: String = publishProperties.getProperty( "repository.password", "" )
publishing {
    repositories {
        maven {
            name = "local"
            url = uri( "$buildDir/repository" )
        }
        maven {
            name = "sonatype"
            url = uri( "https://oss.sonatype.org/service/local/staging/deploy/maven2" ) // Staging repo.
            credentials {
                username = nexusUsername
                password = nexusPassword
            }
        }
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
            artifact( sourcesJar )
            artifact( javadocJar )

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
signing {
    sign( publishing.publications[ "default" ] )
}
tasks.findByName( "signDefaultPublication" )?.doFirst {
    val signingKeyFile = File( uri( publishProperties.getProperty( "signing.keyFile", "" ) ) )
    val signingPassword = publishProperties.getProperty( "signing.password", "" )
    signing.useInMemoryPgpKeys( signingKeyFile.readText(), signingPassword )
}
// Add 'closeAndReleaseRepository' task to close and release uploads to Sonatype Nexus Repository after 'publish' succeeds.
nexusStaging {
    numberOfRetries = 30
    username = nexusUsername
    password = nexusPassword
}
