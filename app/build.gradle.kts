import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.spotless)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

val keystorePropertiesFile = rootProject.file("keystore.properties") // Adjusted path
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

android {
    namespace = "com.sevenspan.calarm"
    compileSdk = 36

    signingConfigs {
        create("release") {
            storeFile =
                file(keystoreProperties.getProperty("storeFile"))
            storePassword = keystoreProperties.getProperty("storePassword") ?: ""
            keyAlias = keystoreProperties.getProperty("keyAlias") ?: ""
            keyPassword = keystoreProperties.getProperty("keyPassword") ?: ""
        }
    }

    defaultConfig {
        applicationId = "com.sevenspan.calarm"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val googleSignInClientId = localProperties.getProperty("GOOGLE_SIGN_IN_SERVER_CLIENT_ID")
                                   ?: "MISSING_IN_LOCAL_PROPERTIES"
        buildConfigField("String", "GOOGLE_SIGN_IN_SERVER_CLIENT_ID", "\"$googleSignInClientId\"")
    }
    packaging {
        resources {
            excludes += "/META-INF/DEPENDENCIES"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi"
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**/*.kt")

        ktlint("1.6.0")
            .customRuleSets(
                listOf(
                    "io.nlopez.compose.rules:ktlint:0.4.16"
                )
            )
            .editorConfigOverride(mapOf("disabled_rules" to "compose:modifier-missing-check"))
        trimTrailingWhitespace()
        endWithNewline()
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose.android)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.database)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.analytics)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.koin.androidx.compose)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.google.http.client.gson)

    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3.android)
    implementation(libs.play.services.auth)
    implementation(libs.triggerx)
    implementation(libs.kotlinx.datetime)
    implementation(libs.koin.androidx.workmanager)

    // Retrofit
    implementation(libs.retrofit) // Or latest
    implementation(libs.converter.gson) // For Gson

    // OkHttp Logging Interceptor (highly recommended for debugging)
    implementation(libs.logging.interceptor) // Or latest
    implementation("io.coil-kt.coil3:coil-compose:3.2.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.2.0")
}