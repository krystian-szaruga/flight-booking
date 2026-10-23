plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    `java-library`
}


dependencies {
    api(project(":application"))
    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.bundles.spring.web)
}