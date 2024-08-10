plugins {
    alias(libs.plugins.pubsub.java.library)
    alias(libs.plugins.pubsub.kotlin.jvm)
    id("java-test-fixtures")
}

dependencies {
    api(projects.server.core.domain)
    api(projects.server.core.data.repository)
}
