plugins {
    id("java")
    application
}

group = "me.seeloewen"
version = "1.0-SNAPSHOT"

application {
    mainClass = "me.seeloewen.Main"
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
    enableAssertions = true
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.joml:joml:1.10.8")
}