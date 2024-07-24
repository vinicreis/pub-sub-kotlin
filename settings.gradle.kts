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
//findProject(":server:core")?.name = "core"

include("server:core:model")
//findProject(":server:core:model")?.name = "model"

include("server:core:service")
//findProject(":server:core:service")?.name = "server-core-service"

include("server:core:grpc")
//findProject(":server:core:service:grpc")?.name = "server-core-service-grpc"

include("server:core:data")
//findProject(":server:core:data")?.name = "server-core-data"

include("server:core:data:model")
//findProject(":server:core:data:model")?.name = "server-core-data-model"

include("server:core:data:database")
//findProject(":server:core:data:database")?.name = "server-core-data-database"

include("server:core:data:database:postgres")
//findProject(":server:core:data:database:postgres")?.name = "server-core-data-database-postgres"

include("server:core:data:database:local")
//findProject(":server:core:data:database:local")?.name = "server-core-data-database-local"

include("server:core:data:repository")
//findProject(":server:core:data:repository")?.name = "server-core-data-repository"

include("server:java-app")
//findProject(":server:java-app")?.name = "server-java-app"
