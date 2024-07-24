plugins {
    alias(libs.plugins.pubsub.java.library)
    alias(libs.plugins.pubsub.kotlin.jvm)
    alias(libs.plugins.pubsub.java.database.library)
}

dependencies {
    implementation(projects.server.core.model)
    implementation(projects.server.core.data.repository)
}
