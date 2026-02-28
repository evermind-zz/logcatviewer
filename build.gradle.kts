plugins {
    id("com.android.library") version "8.13.2"
    id("org.jetbrains.kotlin.android") version "2.2.21"
    id("maven-publish")
}

android {
    buildFeatures {
        dataBinding = false
        viewBinding = true
        buildConfig = true
    }

    compileSdk = 36

    defaultConfig {
        minSdk = 19

        val logcatToolkitAuthorityPostfix = ".com.github.evermind_zz.logcat_toolkit.provider"
        manifestPlaceholders["logcatToolkitAuthority"] = "\${applicationId}$logcatToolkitAuthorityPostfix"
        buildConfigField("String", "LOGCAT_TOOLKIT_AUTHORITY_POSTFIX", "\"$logcatToolkitAuthorityPostfix\"")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    namespace = "com.github.logviewer"

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                // optional: if JitPack wants to enforce a group ID
                groupId = "com.github.logviewer"
                artifactId = "logviewer"
                version = "1.0.0"
            }
        }
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
