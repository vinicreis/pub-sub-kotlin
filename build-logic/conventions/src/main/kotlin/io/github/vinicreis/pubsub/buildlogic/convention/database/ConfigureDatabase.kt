package io.github.vinicreis.pubsub.buildlogic.convention.database

import org.gradle.api.Project
import org.gradle.api.plugins.JavaApplication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.provideDelegate

internal fun Project.configureDatabase() {
    val databaseProtocol: String by properties
    val databaseDomain: String by properties
    val databaseDriver: String by properties
    val databaseName: String by properties
    val databaseUser: String by properties
    val databasePassword: String by properties
    val databasePort: String by properties

    extensions.configure<JavaApplication> {
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
}
