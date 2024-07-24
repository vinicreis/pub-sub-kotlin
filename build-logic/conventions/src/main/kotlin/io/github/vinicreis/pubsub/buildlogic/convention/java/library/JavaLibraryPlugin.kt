package io.github.vinicreis.pubsub.buildlogic.convention.java.library

import io.github.vinicreis.pubsub.buildlogic.convention.task.test.configureJUnit
import org.gradle.api.Plugin
import org.gradle.api.Project

class JavaLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            applyPlugins()
            configureJUnit()
        }
    }

    private fun Project.applyPlugins() {
        pluginManager.apply("java-library")
    }
}
