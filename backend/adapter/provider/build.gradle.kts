plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    `java-library`
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(project(":core:domain"))
    implementation(project(":core:application"))
    implementation(libs.spring.boot.starter.web)
}