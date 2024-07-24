plugins {
    id("application")
    alias(libs.plugins.kotlin.jvm)
}

application {
    val databaseProtocol: String by properties
    val databaseDomain: String by properties
    val databaseDriver: String by properties
    val databaseName: String by properties
    val databaseUser: String by properties
    val databasePassword: String by properties
    val databasePort: String by properties

    applicationName = "Pub Sub Server"
    mainClass = "io.github.vinicreis.pubsub.server.MainKt"
    applicationDefaultJvmArgs = mutableListOf(
        "-Ddatabase.protocol=$databaseProtocol",
        "-Ddatabase.domain=$databaseDomain",
        "-Ddatabase.driver=$databaseDriver",
        "-Ddatabase.name=$databaseName",
        "-Ddatabase.user=$databaseUser",
        "-Ddatabase.password=$databasePassword",
        "-Ddatabase.port=$databasePort",
    )
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

dependencies {
    implementation(projects.protos)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.google.protobuf.kotlin)
    implementation(libs.grpc.kotlin.stub)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.okhttp)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)

    implementation(libs.postgres.driver)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}
