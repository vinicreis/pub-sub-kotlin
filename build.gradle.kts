plugins {
    base
    `jacoco-report-aggregation`
}

allprojects {
    afterEvaluate {
        group = "io.github.vinicreis.pubsub"
        version = libs.versions.app.get()
    }
}

dependencies {
    jacocoAggregation(projects.server.javaApp.core)
    jacocoAggregation(projects.client.javaApp.core)
}

reporting {
    reports {
        val testCodeCoverageReport by creating(JacocoCoverageReport::class) {
            testType = TestSuiteType.UNIT_TEST
        }
    }
}

tasks.check {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
}
