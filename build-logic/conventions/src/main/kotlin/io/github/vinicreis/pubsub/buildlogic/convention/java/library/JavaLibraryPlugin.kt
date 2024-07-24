package io.github.vinicreis.pubsub.buildlogic.convention.java.library

import io.github.vinicreis.pubsub.buildlogic.convention.java.config.test.setupTests
import org.gradle.api.Plugin
import org.gradle.api.Project

class JavaLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            applyPlugins()
            setupTests()
        }
    }

    private fun Project.applyPlugins() {
        pluginManager.apply("java-library")
    }
}
