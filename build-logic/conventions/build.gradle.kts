plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlin.jvm)
    implementation(libs.google.protobuf.plugin)
    implementation(libs.test.logger)
}

gradlePlugin {
    plugins {
        register("pubsub.java.application") {
            id = "io.github.vinicreis.pubsub.build.convention.java.application"
            implementationClass = "io.github.vinicreis.pubsub.buildlogic.convention.java.application.JavaApplicationPlugin"
        }

        register("pubsub.java.library") {
            id = "io.github.vinicreis.pubsub.build.convention.java.library"
            implementationClass = "io.github.vinicreis.pubsub.buildlogic.convention.java.library.JavaLibraryPlugin"
        }

        register("pubsub.kotlin.jvm") {
            id = "io.github.vinicreis.pubsub.build.convention.kotlin.jvm"
            implementationClass = "io.github.vinicreis.pubsub.buildlogic.convention.kotlin.jvm.KotlinJvmPlugin"
        }

        register("pubsub.protobuf") {
            id = "io.github.vinicreis.pubsub.build.convention.protobuf"
            implementationClass = "io.github.vinicreis.pubsub.buildlogic.convention.protobuf.ProtobufPlugin"
        }

        register("pubsub.grpc") {
            id = "io.github.vinicreis.pubsub.build.convention.grpc"
            implementationClass = "io.github.vinicreis.pubsub.buildlogic.convention.grpc.GrpcPlugin"
        }

        register("pubsub.java.database.application") {
            id = "io.github.vinicreis.pubsub.build.convention.java.database.application"
            implementationClass = "io.github.vinicreis.pubsub.buildlogic.convention.java.database.JavaApplicationDatabasePlugin"
        }

        register("pubsub.java.database.library") {
            id = "io.github.vinicreis.pubsub.build.convention.java.database.library"
            implementationClass = "io.github.vinicreis.pubsub.buildlogic.convention.java.database.JavaLibraryDatabasePlugin"
        }
    }
}
