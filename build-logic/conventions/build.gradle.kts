plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlin.jvm)
    implementation(libs.google.protobuf.plugin)
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
    }
}
