plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":adapter:web"))
    implementation(project(":adapter:persistence"))

    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.bundles.spring.runtime)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    testImplementation(libs.spring.boot.starter.test)

}