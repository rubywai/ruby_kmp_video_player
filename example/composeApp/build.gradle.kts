import org.gradle.api.tasks.Exec

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

val debugXcFramework = layout.buildDirectory.dir("xcode-frameworks/debug/RubyExampleComposeApp.xcframework")
val debugBridgeXcFramework = layout.buildDirectory.dir("xcode-frameworks/debug/RubyAVPlayerBridge.xcframework")
val libraryProject = project(":library")
val libraryBridgeOutputDirectory = libraryProject.layout.buildDirectory.dir("ruby-avplayer-bridge")

val libraryBridgeIosArm64 = libraryProject.tasks.named("compileRubyAvPlayerBridgeIosArm64")
val libraryBridgeIosSimulatorArm64 = libraryProject.tasks.named("compileRubyAvPlayerBridgeIosSimulatorArm64")

tasks.register<Exec>("assembleDebugXCFramework") {
    notCompatibleWithConfigurationCache("Invokes Apple's xcodebuild tool to package the XCFramework")
    dependsOn("linkDebugFrameworkIosArm64", "linkDebugFrameworkIosSimulatorArm64")

    doFirst {
        debugXcFramework.get().asFile.deleteRecursively()
        debugBridgeXcFramework.get().asFile.deleteRecursively()
    }
    commandLine(
        "/bin/sh",
        "-c",
        "xcodebuild -create-xcframework " +
            "-library '${libraryBridgeOutputDirectory.get().asFile}/arm64-apple-ios15.0/libRubyAVPlayerBridge.a' " +
            "-headers '${libraryProject.layout.projectDirectory.dir("src/nativeInterop/cinterop")}' " +
            "-library '${libraryBridgeOutputDirectory.get().asFile}/arm64-apple-ios15.0-simulator/libRubyAVPlayerBridge.a' " +
            "-headers '${libraryProject.layout.projectDirectory.dir("src/nativeInterop/cinterop")}' " +
            "-output '${debugBridgeXcFramework.get().asFile}' " +
            "&& xcodebuild -create-xcframework " +
            "-framework '${layout.buildDirectory.file("bin/iosArm64/debugFramework/RubyExampleComposeApp.framework").get().asFile}' " +
            "-framework '${layout.buildDirectory.file("bin/iosSimulatorArm64/debugFramework/RubyExampleComposeApp.framework").get().asFile}' " +
            "-output '${debugXcFramework.get().asFile}'",
    )
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "RubyExampleComposeApp"
            isStatic = true

            val bridgeTarget = if (target.name == "iosArm64") {
                "arm64-apple-ios15.0"
            } else {
                "arm64-apple-ios15.0-simulator"
            }
            linkerOpts(
                "-force_load",
                "${libraryBridgeOutputDirectory.get().asFile}/$bridgeTarget/libRubyAVPlayerBridge.a",
            )
        }
    }

    androidLibrary {
        namespace = "io.github.waiphyoaung.rubykmpplayer.example.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    sourceSets {
        commonMain.dependencies {
        implementation(project(":library"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
        androidMain.dependencies {
            implementation(libs.androidx.media3.ui)
        }
    }
}

tasks.matching { it.name.contains("IosArm64") && it.name.startsWith("link") }.configureEach {
    dependsOn(libraryBridgeIosArm64)
}
tasks.matching { it.name.contains("IosSimulatorArm64") && it.name.startsWith("link") }.configureEach {
    dependsOn(libraryBridgeIosSimulatorArm64)
}
