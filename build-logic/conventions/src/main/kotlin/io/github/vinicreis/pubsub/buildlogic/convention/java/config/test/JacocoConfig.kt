package io.github.vinicreis.pubsub.buildlogic.convention.java.config.test

import io.github.vinicreis.pubsub.buildlogic.convention.extension.libs
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

fun Project.configureJacoco() {
    plugins.apply("jacoco")

    tasks.withType(Test::class.java) {
        finalizedBy(
            tasks.withType(JacocoReport::class.java) {
                reports {
                    html.required.set(true)
                }
            }
        )
    }

    tasks.withType(JacocoReport::class.java) {
        dependsOn(tasks.withType(Test::class.java))
    }

    tasks.withType(JacocoCoverageVerification::class.java) {
        violationRules {
            rule {
                limit {
                    minimum = 0.8.toBigDecimal()
                }
            }
        }
    }

    tasks.named("check").configure {
        dependsOn(tasks.withType(JacocoCoverageVerification::class.java))
    }

    extensions.configure(JacocoPluginExtension::class.java) {
        toolVersion = libs.findVersion("jacoco").get().requiredVersion
    }
}
