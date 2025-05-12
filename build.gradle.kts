// Top-level build file where you can add configuration options common to all sub-projects/modules.

// Define plugins and their versions. Applying 'apply false' here
// means these plugins are defined but not applied to the root project itself,
// only to the sub-modules (like 'app') where they are explicitly applied.
plugins {
    id("com.android.application") version "8.2.2" apply false // The Android Application Plugin
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false // The Kotlin Android plugin
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false // The Kotlin Serialization plugin (if used)
    // Add other top-level plugins here with apply false if needed
}

// This block defines the repositories where Gradle looks for plugins themselves.
// It's crucial for finding plugins like com.android.application, org.jetbrains.kotlin.android, etc.
pluginManagement {
    repositories {
        // Google's Maven repository is required for the Android Gradle Plugin
        google()
        // Maven Central is a common repository for many plugins and dependencies
        mavenCentral()
        // If you use plugins from other sources, add their repositories here
    }
}

// This block defines repositories for dependencies used by all sub-projects/modules
// in the project, unless overridden in a module's build.gradle.kts.
// It's often redundant if repositories are defined in app/build.gradle.kts,
// but can be useful for project-wide dependency sources.
allprojects {
    repositories {
        // Google's Maven repository is required for many Android dependencies
        google()
        // Maven Central is a common repository for many dependencies
        mavenCentral()
        // If you use dependencies from other sources, add their repositories here
    }
}

// A common task to clean the project build directories
tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
