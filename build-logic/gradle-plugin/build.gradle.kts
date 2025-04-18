plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.gradle.publish)
    alias(libs.plugins.buildconfig)
}

val bindgenInfo = parseBindgenCargoToml(
    rootProject.layout.projectDirectory.file("../bindgen/Cargo.toml").asFile
)

group = "io.gitlab.trixnity.uniffi"
description = "Gradle UniFFI Plugin"
version = bindgenInfo.version

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(gradleKotlinDsl())
    compileOnly(plugin(libs.plugins.kotlin.jvm))
    implementation(plugin(libs.plugins.android.application))
    implementation(plugin(libs.plugins.android.library))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jna)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotest.assertions.core)
}

tasks.withType<Test> {
    useJUnitPlatform()
    reports {
        junitXml.required.set(true)
    }
}

buildConfig {
    packageName = "io.gitlab.trixnity.uniffi.gradle"

    buildConfigField("String", "BINDGEN_VERSION", "\"${bindgenInfo.version}\"")
    buildConfigField("String", "BINDGEN_CRATE", "\"${bindgenInfo.name}\"")
    buildConfigField("String", "BINDGEN_BIN", "\"${bindgenInfo.binName}\"")

    forClass("DependencyVersions") {
        buildConfigField("String", "OKIO", "\"${libs.versions.okio.get()}\"")
        buildConfigField("String", "KOTLINX_ATOMICFU", "\"${libs.versions.kotlinx.atomicfu.get()}\"")
        buildConfigField("String", "KOTLINX_DATETIME", "\"${libs.versions.kotlinx.datetime.get()}\"")
        buildConfigField("String", "KOTLINX_COROUTINES", "\"${libs.versions.kotlinx.coroutines.get()}\"")
        buildConfigField("String", "JNA", "\"${libs.versions.jna.get()}\"")
    }

    forClass("PluginIds") {
        buildConfigField("String", "KOTLIN_MULTIPLATFORM", "\"${libs.plugins.kotlin.multiplatform.get().pluginId}\"")
        buildConfigField("String", "KOTLIN_ATOMIC_FU", "\"${libs.plugins.kotlin.atomicfu.get().pluginId}\"")
        buildConfigField("String", "ANDROID_APPLICATION", "\"${libs.plugins.android.application.get().pluginId}\"")
        buildConfigField("String", "ANDROID_LIBRARY", "\"${libs.plugins.android.library.get().pluginId}\"")
        buildConfigField("String", "CARGO_KOTLIN_MULTIPLATFORM", "\"io.gitlab.trixnity.cargo.kotlin.multiplatform\"")
    }
}

@Suppress("UnstableApiUsage")
gradlePlugin {
    vcsUrl.set("https://gitlab.com/trixnity/uniffi-kotlin-multiplatform-bindings")
    website.set(vcsUrl)

    plugins {
        create("cargo-kotlin-multiplatform") {
            id = "io.gitlab.trixnity.cargo.kotlin.multiplatform"
            displayName = name
            implementationClass = "io.gitlab.trixnity.gradle.cargo.CargoPlugin"
            description = "A plugin for building Rust libraries and link them to Kotlin projects."
            tags.addAll("rust", "kotlin", "kotlin-multiplatform")
        }
        create("rustlink-kotlin-multiplatform") {
            id = "io.gitlab.trixnity.rustlink.kotlin.multiplatform"
            displayName = name
            implementationClass = "io.gitlab.trixnity.gradle.rustlink.RustLinkPlugin"
            description = "A plugin providing helper functions for linking Rust libraries to Kotlin projects."
            tags.addAll("rust", "kotlin", "kotlin-multiplatform")
        }
        create("uniffi-kotlin-multiplatform") {
            id = "io.gitlab.trixnity.uniffi.kotlin.multiplatform"
            displayName = name
            implementationClass = "io.gitlab.trixnity.gradle.uniffi.UniFfiPlugin"
            description = "A plugin for generating UniFFI Kotlin Multiplatform bindings for Rust libraries."
            tags.addAll("uniffi", "rust", "kotlin", "kotlin-multiplatform")
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHub"
            url = uri("https://maven.pkg.github.com/clebrain/uniffi-kotlin-multiplatform-bindings")
            credentials {
                username = run {
                    project.findProperty("gpr.user") as String?
                        ?: System.getenv("GITHUB_ACTOR")
                        ?: System.getenv("USERNAME")
                }
                password = run {
                    project.findProperty("gpr.key") as String?
                        ?: System.getenv("GITHUB_TOKEN")
                        ?: System.getenv("TOKEN")
                }
            }
        }
    }
}
