plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    `java-library`
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(project(":core:application"))
    implementation(libs.spring.boot.starter.mail)
    implementation(libs.spring.boot.starter.kafka)
    implementation(libs.spring.boot.starter.jackson)
    testImplementation(libs.jackson.module.kotlin)
}
