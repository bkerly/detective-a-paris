import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

dependencies {
    implementation(project(":core"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

val assetsDir = rootProject.file("app/src/main/assets")

application {
    mainClass.set("detective.desktop.Main")
}

// ./gradlew :desktop:run   plays the game in a window (no sound unless you pass a folder of .wav files)
tasks.named<JavaExec>("run") {
    args(assetsDir.path)
}

// ./gradlew :desktop:playthrough   checks the unit files, plays every unit by script and saves pictures of the screens.
// Add unit ids to play only those:  ./gradlew :desktop:playthrough --args="unit3 unit4"
tasks.register<JavaExec>("playthrough") {
    group = "verification"
    description = "Plays the game from start to finish with no person at the controls."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("detective.desktop.Check")
    args(assetsDir.path, layout.buildDirectory.dir("playthrough").get().asFile.path)
    systemProperty("java.awt.headless", "true")
}

// ./gradlew :desktop:lines   writes tools/lines.tsv, the list of French lines that tools/make_audio.R turns into sound
tasks.register<JavaExec>("lines") {
    group = "build"
    description = "Lists every French line in every unit, for the voice script."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("detective.desktop.Lines")
    args(assetsDir.path, rootProject.file("tools/lines.tsv").path)
    systemProperty("java.awt.headless", "true")
}

// ./gradlew :desktop:sheet   draws every sprite, icon and face on one picture, for checking new art
tasks.register<JavaExec>("sheet") {
    group = "verification"
    description = "Draws all the art on one picture."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("detective.desktop.Sheet")
    args(assetsDir.path, layout.buildDirectory.file("art-sheet.png").get().asFile.path)
    systemProperty("java.awt.headless", "true")
}
