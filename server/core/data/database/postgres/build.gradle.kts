plugins {
    alias(libs.plugins.pubsub.java.library)
    alias(libs.plugins.pubsub.kotlin.jvm)
    alias(libs.plugins.pubsub.java.database.library)
}

dependencies {
    implementation(projects.server.core.domain)
    implementation(projects.server.core.data.repository)

    testImplementation(testFixtures(projects.server.core.test))
}
