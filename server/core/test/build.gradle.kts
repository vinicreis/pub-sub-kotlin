plugins {
    alias(libs.plugins.pubsub.java.library)
    alias(libs.plugins.pubsub.kotlin.jvm)
    id("java-test-fixtures")
}

dependencies {
    implementation(projects.server.core.domain)
    implementation(projects.server.core.data.repository)
}
