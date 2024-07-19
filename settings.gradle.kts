@file:Suppress("UnstableApiUsage")
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.PREFER_SETTINGS

    repositories {
        mavenCentral()
    }
}

rootProject.name = "Pub-Sub Distributed System"

include("domain")
include("server")
