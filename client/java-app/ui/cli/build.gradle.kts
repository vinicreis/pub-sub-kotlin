plugins {
    alias(libs.plugins.pubsub.java.library)
    alias(libs.plugins.pubsub.kotlin.jvm)
}

dependencies {
    implementation(projects.client.core.domain)
    implementation(projects.client.core.service)
}
