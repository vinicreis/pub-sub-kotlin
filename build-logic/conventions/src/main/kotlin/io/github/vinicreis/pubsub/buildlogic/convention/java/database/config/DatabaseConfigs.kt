package io.github.vinicreis.pubsub.buildlogic.convention.java.database.config

import io.github.vinicreis.pubsub.buildlogic.convention.extension.implementation
import io.github.vinicreis.pubsub.buildlogic.convention.extension.libs
import org.gradle.api.Project
import org.gradle.api.plugins.JavaApplication
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.provideDelegate

internal fun JavaApplication.setupDatabase(project: Project) {
    val databaseProtocol: String by project.properties
    val databaseDomain: String by project.properties
    val databaseDriver: String by project.properties
    val databaseName: String by project.properties
    val databaseUser: String by project.properties
    val databasePassword: String by project.properties
    val databasePort: String by project.properties

    applicationDefaultJvmArgs = mutableListOf(
        "-Ddatabase.protocol=$databaseProtocol",
        "-Ddatabase.domain=$databaseDomain",
        "-Ddatabase.driver=$databaseDriver",
        "-Ddatabase.name=$databaseName",
        "-Ddatabase.user=$databaseUser",
        "-Ddatabase.password=$databasePassword",
        "-Ddatabase.port=$databasePort",
    )
}

private object Libraries {
    const val EXPOSED_CORE = "exposed.core"
    const val EXPOSED_JDBC = "exposed.jdbc"
    const val EXPOSED_KOTLIN_DATETIME = "exposed.kotlin.datetime"
    const val POSTGRES_DRIVER = "postgres.driver"
}

internal fun Project.applyDatabaseDependencies() {
    dependencies {
        implementation(libs.findLibrary(Libraries.EXPOSED_CORE).get())
        implementation(libs.findLibrary(Libraries.EXPOSED_JDBC).get())
        implementation(libs.findLibrary(Libraries.EXPOSED_KOTLIN_DATETIME).get())
        implementation(libs.findLibrary(Libraries.POSTGRES_DRIVER).get())
    }
}
