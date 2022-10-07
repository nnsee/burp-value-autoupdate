import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    id("org.graalvm.plugin.compiler") version "0.1.0-alpha2"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "ee.nns.updater"
version = "0.0.1"

graal {
    version = "22.2.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.20")
    implementation("com.miglayout:miglayout-swing:11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
    implementation("com.google.re2j:re2j:1.7")
    implementation("org.graalvm.sdk:graal-sdk:22.2.0")
    implementation("org.graalvm.truffle:truffle-api:22.2.0")
    implementation("org.graalvm.js:js:22.2.0")
    api("net.portswigger.burp.extender:montoya-api:0.9.25")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<ShadowJar> {
    exclude("**/*.kotlin_metadata")
    exclude("**/*.kotlin_module")
    exclude("META-INF/maven/**")

    minimize()

    dependencies {
        exclude(dependency("net.portswigger.burp.extender:montoya-api:.*"))
    }
}