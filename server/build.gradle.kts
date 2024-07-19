plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "io.github.vinicreis"
version = libs.versions.app.get()

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}
