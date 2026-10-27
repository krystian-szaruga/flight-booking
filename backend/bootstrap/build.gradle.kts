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

    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.bundles.spring.runtime)
    implementation(libs.bundles.flyway)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.extensions.spring)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.keycloak)

}