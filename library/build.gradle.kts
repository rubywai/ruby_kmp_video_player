import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.Exec

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "io.github.rubywai"
version = "1.1.1"

val bridgeSource = layout.projectDirectory.file("src/nativeInterop/cinterop/RubyAVPlayerBridge.m")
val bridgeOutputDirectory = layout.buildDirectory.dir("ruby-avplayer-bridge")

fun registerRubyAvPlayerBridgeTask(
    name: String,
    sdk: String,
    target: String,
) = tasks.register<Exec>(name) {
    val outputDirectory = bridgeOutputDirectory.get().asFile
    val objectFile = outputDirectory.resolve("$target/RubyAVPlayerBridge.o")
    val staticLibrary = outputDirectory.resolve("$target/libRubyAVPlayerBridge.a")

    outputs.file(staticLibrary)
    doFirst {
        outputDirectory.resolve(target).mkdirs()
        staticLibrary.delete()
    }
    commandLine(
        "/bin/sh",
        "-c",
        "xcrun --sdk $sdk clang -target $target -fobjc-arc -Isrc/nativeInterop/cinterop -c '${bridgeSource.asFile}' -o '$objectFile' " +
            "&& xcrun --sdk $sdk ar -rcs '$staticLibrary' '$objectFile'",
    )
}

val rubyAvPlayerBridgeIosArm64 = registerRubyAvPlayerBridgeTask(
    "compileRubyAvPlayerBridgeIosArm64",
    "iphoneos",
    "arm64-apple-ios15.0",
)
val rubyAvPlayerBridgeIosSimulatorArm64 = registerRubyAvPlayerBridgeTask(
    "compileRubyAvPlayerBridgeIosSimulatorArm64",
    "iphonesimulator",
    "arm64-apple-ios15.0-simulator",
)

tasks.matching { it.name == "cinteropRubyAVPlayerBridgeIosArm64" }.configureEach {
    dependsOn(rubyAvPlayerBridgeIosArm64)
}
tasks.matching { it.name == "cinteropRubyAVPlayerBridgeIosSimulatorArm64" }.configureEach {
    dependsOn(rubyAvPlayerBridgeIosSimulatorArm64)
}

kotlin {
    androidLibrary {
        namespace = "io.github.waiphyoaung.rubykmpplayer"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withJava() // enable java compilation support
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    iosArm64 {
        compilations.getByName("main").cinterops.create("rubyAVPlayerBridge") {
            definitionFile.set(layout.projectDirectory.file("src/nativeInterop/cinterop/RubyAVPlayerBridgeIosArm64.def"))
        }
        binaries.all {
            linkerOpts(
                "-force_load",
                "${bridgeOutputDirectory.get().asFile}/arm64-apple-ios15.0/libRubyAVPlayerBridge.a",
                "-framework",
                "CoreMedia",
            )
        }
    }
    iosSimulatorArm64 {
        compilations.getByName("main").cinterops.create("rubyAVPlayerBridge") {
            definitionFile.set(layout.projectDirectory.file("src/nativeInterop/cinterop/RubyAVPlayerBridgeIosSimulatorArm64.def"))
        }
        binaries.all {
            linkerOpts(
                "-force_load",
                "${bridgeOutputDirectory.get().asFile}/arm64-apple-ios15.0-simulator/libRubyAVPlayerBridge.a",
                "-framework",
                "CoreMedia",
            )
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            api(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            api(compose.ui)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.exoplayer.hls)
            implementation(libs.androidx.media3.ui)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }

    coordinates(group.toString(), "ruby-kmp-player", version.toString())

    pom {
        name = "Ruby KMP Player"
        description = "A reusable Kotlin Multiplatform video player library backed by ExoPlayer on Android and AVPlayer on iOS."
        inceptionYear = "2026"
        url = "https://github.com/rubywai/ruby_kmp_video_player"
        licenses {
            license {
                name = "Apache-2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "rubywai"
                name = "Wai Phyo Aung"
                email = "newwaiphyo33@gmail.com"
                organization = "Rubywai"
                organizationUrl = "https://github.com/rubywai"
                url = "https://github.com/rubywai"
            }
        }
        scm {
            url = "https://github.com/rubywai/ruby_kmp_video_player"
            connection = "scm:git:git://github.com/rubywai/ruby_kmp_video_player.git"
            developerConnection = "scm:git:ssh://git@github.com/rubywai/ruby_kmp_video_player.git"
        }
    }
}
