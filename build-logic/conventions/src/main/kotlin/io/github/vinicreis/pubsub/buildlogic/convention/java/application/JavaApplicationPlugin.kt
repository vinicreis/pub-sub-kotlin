package io.github.vinicreis.pubsub.buildlogic.convention.java.application

import io.github.vinicreis.pubsub.buildlogic.convention.database.configureDatabase
import io.github.vinicreis.pubsub.buildlogic.convention.task.test.configureJUnit
import org.gradle.api.Plugin
import org.gradle.api.Project

class JavaApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            applyPlugins()
            configureDatabase()
            configureJUnit()
        }
    }

    private fun Project.applyPlugins() {
        pluginManager.apply("application")
    }
}
