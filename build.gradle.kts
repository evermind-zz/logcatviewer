buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        //noinspection UseTomlInstead
        classpath("com.android.tools.build:gradle:8.12.0")
    }
}

repositories {
    google()
    mavenCentral()
}

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    buildFeatures {
        dataBinding = false
        viewBinding = true
    }

    compileSdk = 34

    defaultConfig {
        minSdk = 19
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    namespace = "com.github.logviewer"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    //noinspection UseTomlInstead
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    api("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.2.1")
}
