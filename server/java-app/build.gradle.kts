plugins {
    alias(libs.plugins.pubsub.java.application)
    alias(libs.plugins.pubsub.kotlin.jvm)
    alias(libs.plugins.pubsub.protobuf)
    alias(libs.plugins.pubsub.grpc)
}

application {
    applicationName = "Pub Sub Server"
    mainClass = "io.github.vinicreis.pubsub.server.java.app.MainKt"
}

dependencies {
    implementation(projects.server.core.model)
    implementation(projects.server.core.service)
    implementation(projects.server.core.grpc)
    implementation(projects.server.core.data.model)
    implementation(projects.server.core.data.repository)
    implementation(projects.server.core.data.database.local)
    implementation(projects.server.core.data.database.postgres)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
}
