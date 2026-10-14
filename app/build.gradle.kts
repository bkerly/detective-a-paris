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
    // offline French speech recognition for the spoken battles (Apache 2.0)
    implementation("com.alphacephei:vosk-android:0.3.47@aar")
    implementation("net.java.dev.jna:jna:5.13.0@aar")
}

// The small French speech model (Vosk, Apache 2.0, about 41 MB) is not kept in git.
// The first build fetches it into src/main/assets/model-fr. Without it the game still
// works: in the spoken battles she says each phrase aloud and taps.
val getSpeechModel by tasks.registering {
    description = "Fetches the small French Vosk model into assets/model-fr, if it is missing."
    val target = layout.projectDirectory.dir("src/main/assets/model-fr").asFile
    val tmp = layout.buildDirectory.dir("speech-model").get().asFile
    onlyIf { !File(target, "uuid").exists() }
    doLast {
        val url = "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip"
        try {
            tmp.mkdirs()
            val zip = File(tmp, "model.zip")
            java.net.URI(url).toURL().openStream().use { input -> zip.outputStream().use { input.copyTo(it) } }
            target.deleteRecursively()
            target.mkdirs()
            java.util.zip.ZipInputStream(zip.inputStream()).use { zin ->
                var e = zin.nextEntry
                while (e != null) {
                    // drop the zip's own top folder
                    val name = e.name.substringAfter('/')
                    if (name.isNotEmpty()) {
                        val out = File(target, name)
                        if (!out.canonicalPath.startsWith(target.canonicalPath)) throw GradleException("odd path in zip: ${e.name}")
                        if (e.isDirectory) out.mkdirs()
                        else { out.parentFile.mkdirs(); out.outputStream().use { zin.copyTo(it) } }
                    }
                    e = zin.nextEntry
                }
            }
            // the app copies the model out of the APK once, and again only when this changes
            File(target, "uuid").writeText(java.util.UUID.randomUUID().toString())
            zip.delete()
            logger.lifecycle("Speech model saved in $target")
        } catch (e: Exception) {
            target.deleteRecursively()
            logger.warn("Could not fetch the speech model ($e). The game will ask her to say phrases aloud and tap.")
        }
    }
}
tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(getSpeechModel) }
