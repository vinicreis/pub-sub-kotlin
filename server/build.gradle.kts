plugins {
    id("application")
    alias(libs.plugins.kotlin.jvm)
}

application {
    applicationName = "Pub Sub Server"
    mainClass = "io.github.vinicreis.pubsub.server.MainKt"
}

dependencies {
    implementation(project(":protos"))
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.google.protobuf.kotlin)
    implementation(libs.grpc.kotlin.stub)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.okhttp)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}
