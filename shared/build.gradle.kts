plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    android {
        namespace = "com.prism.music.shared"
        compileSdk = 37
        minSdk = 26
    }

    jvm()
}