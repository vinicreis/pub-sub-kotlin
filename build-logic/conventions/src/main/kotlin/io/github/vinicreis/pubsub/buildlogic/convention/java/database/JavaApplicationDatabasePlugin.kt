package io.github.vinicreis.pubsub.buildlogic.convention.java.database

import io.github.vinicreis.pubsub.buildlogic.convention.java.database.config.applyDatabaseDependencies
import io.github.vinicreis.pubsub.buildlogic.convention.java.database.config.setupDatabase
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaApplication
import org.gradle.kotlin.dsl.configure

class JavaApplicationDatabasePlugin : Plugin<Project> {
    private val Project.isJavaApplication: Boolean
        get() = pluginManager.hasPlugin("application")

    override fun apply(target: Project) {
        with(target) {
            ensureIsJavaApplication()
//            configureDatabase()
            applyDatabaseDependencies()
        }
    }

    private fun Project.ensureIsJavaApplication() {
        check(isJavaApplication) { "Database can only be configured on a Java application" }
    }

    private fun Project.configureDatabase() {
        extensions.configure<JavaApplication> {
            setupDatabase(this@configureDatabase)
        }
    }
}
