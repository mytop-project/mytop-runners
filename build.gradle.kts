plugins {
    kotlin("jvm") version "2.2.21" apply false
}

// Every module is plain JVM (the standard library only), so its tests run on a build machine with no device and no
// Android SDK, and the code builds for droidtop on Android and for Pythia and droidtop on Linux alike.
subprojects {
    group = "dev.droidtop.shared"
    version = rootProject.property("lib.version") as String
}
