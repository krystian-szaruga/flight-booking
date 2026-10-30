plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}


rootProject.name = "backend"
include("core:domain")
include("core:application")
include("adapter:web")
include("adapter:persistence")
include("bootstrap")
include("adapter:provider")
include("adapter:scheduler")
include("adapter:security")
include("adapter:mail")
include("adapter:payment")