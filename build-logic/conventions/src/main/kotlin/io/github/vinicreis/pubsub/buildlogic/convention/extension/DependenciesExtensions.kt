package io.github.vinicreis.pubsub.buildlogic.convention.extension

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun DependencyHandlerScope.implementation(dependency: MinimalExternalModuleDependency) {
    "implementation"(dependency)
}

internal fun DependencyHandlerScope.testImplementation(dependency: MinimalExternalModuleDependency) {
    "testImplementation"(dependency)
}
