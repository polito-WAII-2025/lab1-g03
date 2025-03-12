plugins {
    kotlin("jvm") version "2.1.10"
    id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(22)
}

ktlint {
    version.set("0.45.2") // Use the latest version
    android.set(true) // Set to true for Android projects, false for pure Kotlin projects
    ignoreFailures.set(false) // Fail the build if lint issues are found
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

tasks.named("check") {
    dependsOn("ktlintCheck")
}
