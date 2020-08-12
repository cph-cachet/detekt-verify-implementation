val detektVersion = "1.9.1"


plugins {
    kotlin("jvm") version "1.3.72"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.gitlab.arturbosch.detekt:detekt-api:$detektVersion")

    testImplementation("io.gitlab.arturbosch.detekt:detekt-test:$detektVersion")
}
