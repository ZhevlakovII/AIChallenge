rootProject.name = "AIChallenge"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

pluginManagement {
    includeBuild("build-logic")

    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

/**
 * Auto-include discovered modules under target roots
 * Split to new and old targets
 */
val targetRoots = listOf(
    "core",
    "designsystem",
    "tools",
    "features",
    "targets",
    // TODO("It's old. Remove after migrate to v3")
    "instruments",
    // TODO("It's old! Remove after migrate to v2")
    "composeApp",
    "ticketmanager",
    "instances",
    "shared",
    "rag",
)

// Include modules from target roots
targetRoots.forEach(::includeModulesUnder)

fun includeModulesUnder(root: String) {
    val rootDirFile = file(root)
    if (!rootDirFile.exists()) return

    rootDirFile.walkTopDown()
        .onEnter { dir ->
            val name = dir.name
            // skip service/build folders and composite build
            if (name == "build" || name == ".gradle" || name == ".git" || name == "gradle") return@onEnter false
            if (dir.toPath().startsWith(file("build-logic").toPath())) return@onEnter false
            true
        }
        .filter { it.isDirectory && it.isGradleModuleDir() }
        .forEach { moduleDir ->
            val relPath = moduleDir.relativeTo(file(".")).path.replace(File.separatorChar, '/')
            val projectPath = ":" + relPath.replace("/", ":")
            if (findProject(projectPath) == null) {
                include(projectPath)
            }
        }
}


fun File.isGradleModuleDir(): Boolean =
    File(this, "build.gradle.kts").exists() || File(this, "build.gradle").exists()
