plugins {
    alias(libs.plugins.pubsub.java.application)
    alias(libs.plugins.pubsub.kotlin.jvm)
    alias(libs.plugins.pubsub.protobuf)
    alias(libs.plugins.pubsub.grpc)
    alias(libs.plugins.pubsub.java.database.application)
}

application {
    applicationName = "pub-sub-server"
    mainClass = "io.github.vinicreis.pubsub.server.java.app.MainKt"
}

dependencies {
    implementation(projects.server.core.model)
    implementation(projects.server.core.service)
    implementation(projects.server.core.grpc)
    implementation(projects.server.core.data.model)
    implementation(projects.server.core.data.repository)
    implementation(projects.server.core.data.database.postgres)
}
