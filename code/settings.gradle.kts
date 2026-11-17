pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "code"
include(":app")

gradle.beforeProject {
    val relPath = project.path.replace(':', '/')
    val nonOneDriveBuildDir = File("C:/AndroidBuilds/CMPUT301F26olivesProject$relPath/build")
    project.layout.buildDirectory.set(nonOneDriveBuildDir)
}
 