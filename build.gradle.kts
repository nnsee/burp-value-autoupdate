import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    id("org.graalvm.plugin.compiler") version "0.1.0-alpha2"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "ee.nns.updater"
version = "0.2.0"

graal {
    version = "22.3.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.20")
    implementation("com.miglayout:miglayout-swing:11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("com.google.re2j:re2j:1.7")
    implementation("org.graalvm.sdk:graal-sdk:22.3.0")
    implementation("org.graalvm.truffle:truffle-api:22.3.0")
    implementation("org.graalvm.js:js:22.3.0")
    api("net.portswigger.burp.extensions:montoya-api:0.10.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
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