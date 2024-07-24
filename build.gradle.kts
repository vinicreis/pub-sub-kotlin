allprojects {
    afterEvaluate {
        group = "io.github.vinicreis.pubsub"
        version = libs.versions.app.get()
    }
}
