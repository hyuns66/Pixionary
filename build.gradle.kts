// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
     val objectboxVersion by extra("4.0.2") // For KTS build scripts

    repositories {
        mavenCentral()
    }

    dependencies {
        val nav_version = "2.8.5"
        // Android Gradle Plugin 4.1.0 or later supported
        classpath("com.android.tools.build:gradle:8.1.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
        classpath("io.objectbox:objectbox-gradle-plugin:$objectboxVersion")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$nav_version")
    }
}

plugins {
    id("com.android.application") version "8.1.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}