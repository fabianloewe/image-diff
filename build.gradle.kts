plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.21"
}

group = "com.github.fabianloewe"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("io.insert-koin:koin-core:3.5.3")
    implementation("com.github.ajalt.clikt:clikt:4.2.2")
    implementation("com.sksamuel.scrimage:scrimage-core:4.1.1")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.9.3")
    implementation("me.tongfei:progressbar:0.10.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(20)
}