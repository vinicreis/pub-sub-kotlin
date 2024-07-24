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
        }
    }

    private fun Project.applyPlugins() {
        pluginManager.apply {
            apply(libs.findPlugin(KOTLIN_PLUGIN_ALIAS).get().get().pluginId)
        }
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
            implementation(libs.findLibrary(Libraries.KOTLINX_COROUTINES_CORE).get().get())
        }
    }

    companion object {
        private const val KOTLIN_PLUGIN_ALIAS = "kotlin.jvm"

        private object Libraries {
            const val KOTLINX_COROUTINES_CORE = "kotlinx.coroutines.core"
        }
    }
}