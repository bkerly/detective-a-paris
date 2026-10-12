import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.bkerly.detective"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.bkerly.detective"
        // 22 is Android 5.1, which is Fire OS 5: the oldest Fire tablets still in use
        minSdk = 22
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    androidResources {
        // sound files are played straight out of the APK, so they must not be compressed
        noCompress += "ogg"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))
}
