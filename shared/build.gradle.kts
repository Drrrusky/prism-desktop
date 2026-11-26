plugins {

    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    android {
        namespace = "com.prism.music.shared"
        compileSdk = 37
        minSdk = 26
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.runtime:runtime:1.12.2")
            implementation("org.jetbrains.compose.foundation:foundation:1.12.2")
            implementation("org.jetbrains.compose.material3:material3:1.12.0-alpha03")
            implementation("org.jetbrains.compose.ui:ui:1.12.2")
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            // Java Sound SPI decoder for MP3 input.
            implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4")
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.prism.music.desktop.MainKt"
        nativeDistributions {
            packageName = "Prism"
            packageVersion = "1.0.0"
        }
    }
}
