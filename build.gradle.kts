// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
     val objectboxVersion by extra("4.0.2") // For KTS build scripts

    repositories {
        mavenCentral()
    }

    dependencies {
        // Android Gradle Plugin 4.1.0 or later supported
        classpath("com.android.tools.build:gradle:8.1.0")
        classpath("io.objectbox:objectbox-gradle-plugin:$objectboxVersion")
    }
}

plugins {
    id("com.android.application") version "8.1.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
}