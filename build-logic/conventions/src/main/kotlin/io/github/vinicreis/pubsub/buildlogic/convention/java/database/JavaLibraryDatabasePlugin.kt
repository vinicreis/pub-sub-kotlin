package io.github.vinicreis.pubsub.buildlogic.convention.java.database

import io.github.vinicreis.pubsub.buildlogic.convention.java.database.config.applyDatabaseDependencies
import org.gradle.api.Plugin
import org.gradle.api.Project

class JavaLibraryDatabasePlugin : Plugin<Project> {
    private val Project.isJavaLibrary: Boolean
        get() = pluginManager.hasPlugin("java-library")

    override fun apply(target: Project) {
        with(target) {
            ensureIsJavaLibrary()
            applyDatabaseDependencies()
        }
    }

    private fun Project.ensureIsJavaLibrary() {
        check(isJavaLibrary) { "Database can only be configured on a Java application" }
    }
}
