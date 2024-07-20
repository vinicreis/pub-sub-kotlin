plugins {
    id("application")
    alias(libs.plugins.kotlin.jvm)
}

application {
    applicationName = "Pub Sub Client"
    mainClass = "io.github.vinicreis.pubsub.client.MainKt"
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
}

tasks.test {
    useJUnitPlatform()
}
