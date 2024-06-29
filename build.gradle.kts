import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.ByteArrayOutputStream

plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    id("io.github.goooler.shadow") version "8.1.7"
}

group = "ee.nns.updater"
version = "0.3.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")
    implementation("com.miglayout:miglayout-swing:11.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("com.google.re2j:re2j:1.7")
    implementation("org.graalvm.sdk:graal-sdk:24.0.1")
    implementation("org.graalvm.truffle:truffle-api:24.0.1")
    implementation("org.graalvm.js:js:23.0.4")
    api("net.portswigger.burp.extensions:montoya-api:2023.3")
}

fun getGitCommitHash(): String {
    val outputStream = ByteArrayOutputStream()
    val result = project.exec {
        commandLine("git", "rev-parse", "HEAD")
        standardOutput = outputStream
    }
    return if (result.exitValue == 0)
        outputStream.toString("UTF-8").trim()
    else
        "0000000000000000000000000000000000000000"
}

val commitHash = getGitCommitHash()

val generateBuildConfigFile by tasks.registering(Task::class) {
    // surely there has to be a better way to do this
    doLast {
        val targetFile = file("src/main/kotlin/BuildConfig.kt")
        targetFile.writeText("""
            package burp

            const val COMMIT_HASH = "$commitHash"
            const val VERSION = "$version"
        """.trimIndent())
    }
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
}

tasks.withType<KotlinCompile> {
    dependsOn(generateBuildConfigFile)
}

tasks.withType<ShadowJar> {
    exclude("**/*.kotlin_metadata")
    exclude("**/*.kotlin_module")
    exclude("META-INF/maven/**")

    minimize {
        exclude(dependency("org.graalvm.truffle:.*"))
        exclude(dependency("org.graalvm.js:.*"))
    }

    mergeServiceFiles()

    dependencies {
        exclude(dependency("net.portswigger.burp.extender:montoya-api:.*"))
    }
}
