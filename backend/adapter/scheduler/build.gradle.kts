plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    `java-library`
}


dependencies {
    api(project(":core:application"))
    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.bundles.spring.web)
    testImplementation(libs.spring.boot.starter.test)
}