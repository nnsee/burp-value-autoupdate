import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.nio.file.Files
import java.nio.file.Paths

plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    id("io.github.goooler.shadow") version "8.1.7"
}

group = "ee.nns.updater"
version = "0.3.1"

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
    api("net.portswigger.burp.extensions:montoya-api:2023.12.1")
}

fun getGitCommitHash(): String {
    return try {
        val ref = Files.readAllLines(Paths.get(".git/HEAD"))[0].split(": ")[1]
        Files.readAllLines(Paths.get(".git/$ref"))[0].trim()
    } catch (e: Exception) {
        "unknown"
    }
}

tasks.processResources {
    inputs.property("version", version)
    inputs.property("commitHash", getGitCommitHash())
    filesMatching("version.properties") {
        expand(mapOf("commitHash" to getGitCommitHash(), "version" to version))
    }
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
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
