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
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    testImplementation(libs.postgresql)
}