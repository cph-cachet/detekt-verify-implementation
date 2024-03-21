import java.io.File
import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "dk.cachet.detekt.extensions"
version = "1.2.5"

val jvmTarget = "1.8"
val detektVersion = "1.22.0"
val junit5Version = "5.9.1"
val spek2Version = "2.0.19"


plugins {
    kotlin( "jvm" ) version "1.8.0"
    id( "org.jetbrains.dokka" ) version "1.7.20"
    `maven-publish`
    signing
    id( "io.github.gradle-nexus.publish-plugin" ) version "1.1.0"
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

    withType<JavaCompile> {
        this.targetCompatibility = jvmTarget
    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = jvmTarget
    }

    dokkaHtml {
        outputDirectory.set(layout.projectDirectory.asFile.resolve("dokka"))
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
            url = uri( "${layout.projectDirectory}/repository" )
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
    val isSigningSetUp = publishProperties.propertyNames().toList().isNotEmpty()
    if (!isSigningSetUp) return@signing

    val signingKeyFile = File( uri( publishProperties.getProperty( "signing.keyFile", "" ) ) )
    val signingPassword = publishProperties.getProperty( "signing.password", "" )
    signing.useInMemoryPgpKeys( signingKeyFile.readText(), signingPassword )
    sign( publishing.publications[ "default" ] )
}
nexusPublishing {
    repositories {
        sonatype {
            username.set( nexusUsername )
            password.set( nexusPassword )
        }
    }
}
