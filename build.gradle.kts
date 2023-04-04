import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.io.ByteArrayOutputStream

plugins {
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.serialization") version "1.8.10"
    id("org.graalvm.plugin.compiler") version "0.1.0-alpha2"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "ee.nns.updater"
version = "0.2.1"

graal {
    version = "22.3.1"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.10")
    implementation("com.miglayout:miglayout-swing:11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("com.google.re2j:re2j:1.7")
    implementation("org.graalvm.sdk:graal-sdk:22.3.1")
    implementation("org.graalvm.truffle:truffle-api:22.3.1")
    implementation("org.graalvm.js:js:22.3.1")
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

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
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