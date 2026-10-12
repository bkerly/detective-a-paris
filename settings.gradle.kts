pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DetectiveParis"

// core    the whole game, in plain Kotlin with no Android code
// app     the Android app: a thin shell round core
// desktop a window to play in on a computer, and the scripted playthrough check
include(":core", ":app", ":desktop")
