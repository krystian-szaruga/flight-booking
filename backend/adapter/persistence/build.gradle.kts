plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    `java-library`
}

dependencies {
    api(project(":core:application"))
    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(kotlin("reflect"))
    implementation(libs.bundles.flyway)
    runtimeOnly(libs.postgresql)
}