allprojects {
    afterEvaluate {
        group = "io.github.vinicreis.pubsub"
        version = libs.versions.app.get()
    }
}

tasks.register("clean") {
    group = "build"
    description = "Deletes all build directory"

    subprojects.forEach {
        it.tasks.findByName("clean")?.let { cleanTask ->
            dependsOn(cleanTask)
        }
    }
}
