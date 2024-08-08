plugins {
    alias(libs.plugins.pubsub.java.library)
    alias(libs.plugins.pubsub.kotlin.jvm)
    alias(libs.plugins.pubsub.protobuf)
    alias(libs.plugins.pubsub.grpc)
}

dependencies {
    implementation(projects.protos)
    implementation(projects.client.core.domain)
    implementation(projects.client.core.service)
    implementation(projects.client.core.util)
}
