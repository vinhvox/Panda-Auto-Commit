plugins {
    id("org.jetbrains.intellij.platform") version "2.0.0"
    kotlin("jvm") version "2.1.0"
}

group = "com.viotech"
// 1. CẬP NHẬT PHIÊN BẢN (Bắt buộc khi upload bản mới)
version = "0.0.5"

repositories {
    google()
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create("IC", "2024.3")
        instrumentationTools()
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
        bundledPlugin("com.intellij.java")
    }
    implementation("org.json:json:20230227")
    testImplementation("junit:junit:4.13.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.patchPluginXml {
    sinceBuild.set("241")
    untilBuild.set("253.*")
}

tasks.withType<org.jetbrains.intellij.platform.gradle.tasks.BuildSearchableOptionsTask>().configureEach {
    enabled = false
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}