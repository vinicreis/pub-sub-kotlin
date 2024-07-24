plugins {
    alias(libs.plugins.pubsub.java.library)
    alias(libs.plugins.pubsub.kotlin.jvm)
}

dependencies {
    implementation(projects.server.core.model)
    implementation(projects.server.core.data.repository)
//    implementation(projects.server.core.data.repository)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
}
