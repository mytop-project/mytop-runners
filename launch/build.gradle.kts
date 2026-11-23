plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

sourceSets {
    main {
        // A container's architecture and translator (mytop-project/mytop-containers :arch, pinned as vendor/mytop-containers).
        kotlin.srcDir("../vendor/mytop-containers/arch/src/main/kotlin")
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
