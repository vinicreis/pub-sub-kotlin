plugins {
    alias(libs.plugins.pubsub.java.library)
    alias(libs.plugins.pubsub.kotlin.jvm)
    alias(libs.plugins.pubsub.protobuf)
    alias(libs.plugins.pubsub.grpc)
}

dependencies {
    api(projects.protos)
    implementation(projects.server.core.model)
    implementation(projects.server.core.service)
    implementation(projects.server.core.data.repository)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
}
