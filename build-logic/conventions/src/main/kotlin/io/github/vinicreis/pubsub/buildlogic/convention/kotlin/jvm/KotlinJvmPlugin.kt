package io.github.vinicreis.pubsub.buildlogic.convention.kotlin.jvm

import io.github.vinicreis.pubsub.buildlogic.convention.extension.implementation
import io.github.vinicreis.pubsub.buildlogic.convention.extension.libs
import io.github.vinicreis.pubsub.buildlogic.convention.model.Constants
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class KotlinJvmPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            applyPlugins()
            configureKotlin()
            dependencies()
            testDependencies()
        }
    }

    private fun Project.applyPlugins() {
        pluginManager.apply(Plugins.KOTLIN_JVM)
    }

    private fun Project.configureKotlin() {
        extensions.configure<KotlinJvmProjectExtension>("kotlin") {
            jvmToolchain(Constants.JVM_TARGET)

            compilerOptions {
                freeCompilerArgs.add("-Xcontext-receivers")
            }
        }
    }

    private fun Project.dependencies() {
        dependencies {
            implementation(libs.findLibrary(Libraries.KOTLINX_COROUTINES_CORE).get())
            implementation(libs.findLibrary(Libraries.KOTLINX_DATETIME).get())
        }
    }

    private fun Project.testDependencies() {
        dependencies {
            implementation(libs.findLibrary(Libraries.KOTLINX_COROUTINES_TEST).get())
        }
    }

    companion object {
        private object Plugins {
            const val KOTLIN_JVM = "org.jetbrains.kotlin.jvm"
        }

        private object Libraries {
            const val KOTLINX_COROUTINES_CORE = "kotlinx.coroutines.core"
            const val KOTLINX_DATETIME = "kotlinx.datetime"
            const val KOTLINX_COROUTINES_TEST = "kotlinx.coroutines.test"
        }
    }
}