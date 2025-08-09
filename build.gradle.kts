import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

group = "dk.cachet.detekt.extensions"
version = "1.2.7"

val jvmTarget = "1.8"
val detektVersion = "1.23.8"
val junit5Version = "5.12.2"
val spek2Version = "2.0.19"


plugins {
    kotlin( "jvm" ) version "2.2.0"
    id( "org.jetbrains.dokka" ) version "2.0.0"
    id( "io.gitlab.arturbosch.detekt" ) version "1.23.8"
    `maven-publish`
    signing
    id( "io.github.gradle-nexus.publish-plugin" ) version "2.0.0"
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

    detektPlugins( "io.gitlab.arturbosch.detekt:detekt-rules-ruleauthors:$detektVersion" )
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom( "$projectDir/detekt.yml" )
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
    withType<KotlinJvmCompile> {
        compilerOptions.jvmTarget.set( JvmTarget.fromTarget( jvmTarget ) )
    }
}
dokka {
    dokkaPublications.html {
        outputDirectory.set( layout.buildDirectory.dir( "dokka" ) )
    }
}
tasks.withType<DokkaGenerateTask>().configureEach {
    // HACK: Dokka 2.0.0 exposes this debug file by default (https://github.com/Kotlin/dokka/issues/3958)
    @OptIn( InternalDokkaGradlePluginApi::class )
    dokkaConfigurationJsonFile.convention( null as RegularFile? )
}
val sourcesJar by tasks.registering( Jar::class )
{
    archiveClassifier.set( "sources" )
    from( sourceSets.getByName( "main" ).allSource )
}
val javadocJar by tasks.registering( Jar::class )
{
    archiveClassifier.set( "javadoc" )
    from( tasks.dokkaGeneratePublicationHtml )
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
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username.set( nexusUsername )
            password.set( nexusPassword )
        }
    }
}
