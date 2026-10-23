plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}


rootProject.name = "backend"
include("domain")
include("application")
include("adapter:web")
include("adapter:persistence")
include("bootstrap")