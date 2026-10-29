plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    `java-library`
}


dependencies {
    api(project(":core:application"))
    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.bundles.spring.web)
    implementation(libs.spring.boot.starter.security)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.validation)
    testImplementation(libs.postgresql)
}