import org.gradle.api.tasks.Exec

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

val debugXcFramework = layout.buildDirectory.dir("xcode-frameworks/debug/RubyExampleComposeApp.xcframework")

tasks.register<Exec>("assembleDebugXCFramework") {
    notCompatibleWithConfigurationCache("Invokes Apple's xcodebuild tool to package the XCFramework")
    dependsOn("linkDebugFrameworkIosArm64", "linkDebugFrameworkIosSimulatorArm64")

    doFirst {
        debugXcFramework.get().asFile.deleteRecursively()
    }
    commandLine(
        "/bin/sh",
        "-c",
        "xcodebuild -create-xcframework " +
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
        }
    }

    androidLibrary {
        namespace = "io.github.waiphyoaung.rubykmpplayer.example.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.rubywai:ruby-kmp-player:1.2.0")
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
        }
    }
}
