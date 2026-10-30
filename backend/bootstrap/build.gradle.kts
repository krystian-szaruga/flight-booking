plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":adapter:web"))
    implementation(project(":adapter:persistence"))
    implementation(project(":adapter:provider"))
    implementation(project(":adapter:scheduler"))
    implementation(project(":adapter:security"))
    implementation(project(":adapter:mail"))
    implementation(project(":adapter:payment"))

    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.bundles.spring.runtime)
    implementation(libs.bundles.flyway)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.security)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.extensions.spring)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.spring.boot.starter.webflux)

}