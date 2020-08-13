import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val jvmTarget = "1.8"
val detektVersion = "1.10.0"
val junit5Version = "5.6.2"
val spek2Version = "2.0.12"


plugins {
    kotlin( "jvm" ) version "1.3.72"
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