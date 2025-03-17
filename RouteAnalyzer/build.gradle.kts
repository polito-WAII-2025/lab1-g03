plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.yaml:snakeyaml:2.0")
    implementation("com.uber:h3:3.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(22)
}

ktlint {
    version.set("1.5.0") // Latest working version
    android.set(false) // Set to false for pure Kotlin projects
    ignoreFailures.set(false) // Fail the build if lint issues are found
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

tasks.named("check") {
    dependsOn("ktlintCheck")
}
