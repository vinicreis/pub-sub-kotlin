package io.github.vinicreis.pubsub.buildlogic.convention.task.test

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

internal fun Project.configureJUnit() {
    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
