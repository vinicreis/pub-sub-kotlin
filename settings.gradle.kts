@file:Suppress("UnstableApiUsage")
pluginManagement {
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.PREFER_SETTINGS

    repositories {
        mavenCentral()
    }
}

rootProject.name = "pubsub-service"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include("protos")
include("server")
include("client")

include("server:core")
include("server:core:model")
include("server:core:service")
include("server:core:grpc")
include("server:core:data")
include("server:core:data:model")
include("server:core:data:database")
include("server:core:data:database:postgres")
include("server:core:data:repository")
include("server:core:test")
include("server:java-app")

include("client:core")
include("client:core:model")
include("client:core:service")
include("client:core:grpc")
include("client:java-app")
