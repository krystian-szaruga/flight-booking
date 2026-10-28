plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    `java-library`
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(project(":core:application"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.security.crypto)
    implementation(libs.nimbus.jose.jwt)
}
