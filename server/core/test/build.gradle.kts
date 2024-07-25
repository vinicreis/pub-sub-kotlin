plugins {
    alias(libs.plugins.pubsub.java.library)
    alias(libs.plugins.pubsub.kotlin.jvm)
    id("java-test-fixtures")
}

dependencies {
    implementation(projects.server.core.model)
    implementation(projects.server.core.data.repository)
}
