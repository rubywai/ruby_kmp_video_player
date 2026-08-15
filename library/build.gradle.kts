import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "io.github.rubywai"
version = "1.0.0"

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
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
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

    signAllPublications()

    coordinates(group.toString(), "ruby-kmp-player", version.toString())

    pom {
        name = "Ruby KMP Player"
        description = "A reusable Kotlin Multiplatform video player library backed by ExoPlayer on Android and AVPlayer on iOS."
        inceptionYear = "2026"
        url = "https://github.com/rubywai/ruby-kmp-player/"
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
                url = "https://github.com/rubywai"
            }
        }
        scm {
            url = "https://github.com/rubywai/ruby-kmp-player/"
            connection = "scm:git:git://github.com/rubywai/ruby-kmp-player.git"
            developerConnection = "scm:git:ssh://git@github.com/rubywai/ruby-kmp-player.git"
        }
    }
}
