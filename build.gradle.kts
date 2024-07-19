plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.google.protobuf) apply false
}

allprojects {
    afterEvaluate {
        group = "io.github.vinicreis.pubsub"
        version = libs.versions.app.get()
    }
}
