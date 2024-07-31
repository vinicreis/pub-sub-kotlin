plugins {
    alias(libs.plugins.pubsub.java.application)
    alias(libs.plugins.pubsub.kotlin.jvm)
    alias(libs.plugins.pubsub.protobuf)
    alias(libs.plugins.pubsub.grpc)
}

application {
    applicationName = "Pub Sub Client"
    mainClass = "io.github.vinicreis.pubsub.client.MainKt"
}

dependencies {
    implementation(projects.protos)
    implementation(projects.client.core.model)
    implementation(projects.client.core.service)
    implementation(projects.client.core.grpc)
    implementation(projects.client.javaApp.ui.cli)
}