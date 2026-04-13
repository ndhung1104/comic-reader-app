import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("androidx.navigation.safeargs")
}

val envProperties = Properties().apply {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile.inputStream().use(::load)
    }
}

fun String.asBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.group09.ComicReader"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.group09.ComicReader"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val baseUrl = envProperties.getProperty("MOBILE_BASE_URL", "http://10.0.2.2:8080/")
        buildConfigField(
            "String",
            "BASE_URL",
            baseUrl.asBuildConfigString()
        )

        buildConfigField(
            "String",
            "PUBLIC_BASE_URL",
            envProperties.getProperty("MOBILE_PUBLIC_BASE_URL", baseUrl).asBuildConfigString()
        )

        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            envProperties.getProperty("MOBILE_GOOGLE_WEB_CLIENT_ID", "").asBuildConfigString()
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.glide)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.play.services.auth)
    implementation(libs.room.runtime)
    implementation(libs.work.runtime)
    annotationProcessor(libs.glide.compiler)
    annotationProcessor(libs.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
