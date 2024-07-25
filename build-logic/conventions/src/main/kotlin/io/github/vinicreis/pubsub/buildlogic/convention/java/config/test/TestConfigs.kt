package io.github.vinicreis.pubsub.buildlogic.convention.java.config.test

import io.github.vinicreis.pubsub.buildlogic.convention.extension.libs
import io.github.vinicreis.pubsub.buildlogic.convention.extension.testImplementation
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

private object Plugins {
    const val TEST_LOGGER = "com.adarshr.test-logger"
}

private object Libraries {
    const val JUNIT_BOM = "junit.bom"
    const val JUNIT_JUPITER = "junit.jupiter"
    const val MOCKK = "mockk"
}

internal fun Project.setupTests() {
    applyTestLogger()
    configureTestDependencies()
    configureJUnit()
}

private fun Project.applyTestLogger() {
    pluginManager.apply(Plugins.TEST_LOGGER)
}

private fun Project.configureTestDependencies() {
    dependencies {
        testImplementation(platform(libs.findLibrary(Libraries.JUNIT_BOM).get()))
        testImplementation(libs.findLibrary(Libraries.JUNIT_JUPITER).get())
        testImplementation(libs.findLibrary(Libraries.MOCKK).get())
    }
}

private fun Project.configureJUnit() {
    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
