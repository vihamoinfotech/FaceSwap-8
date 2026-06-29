plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics)
}

android {
    namespace = "com.facechanger.faceswap.enhance"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.facechanger.faceswap.enhance"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

     implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    implementation("com.google.android.gms:play-services-ads:25.3.0")
    implementation("com.tencent:mmkv-static:1.3.16")

    implementation("com.google.code.gson:gson:2.14.0")
    implementation("com.intuit.sdp:sdp-android:1.1.1")
    implementation("androidx.lifecycle:lifecycle-process:2.10.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.10.0")
    implementation("com.getkeepsafe.relinker:relinker:1.4.5")

    implementation("com.android.installreferrer:installreferrer:2.2")

    implementation(platform("com.google.firebase:firebase-bom:34.13.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation(libs.firebase.crashlytics)

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.glide)
    implementation(libs.play.services.appset)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.intuit.sdp:sdp-android:1.1.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.airbnb.android:lottie:6.4.0")
    implementation("com.revenuecat.purchases:purchases:9.23.1")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("com.facebook.android:facebook-android-sdk:17.0.0")
    implementation(libs.androidx.core.splashscreen)
    implementation("com.google.firebase:firebase-messaging")
    implementation("androidx.work:work-runtime:2.9.0")

    // Retrofit + OkHttp (network layer)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Media3 ExoPlayer (in-app video playback)
    implementation("androidx.media3:media3-exoplayer:1.6.1")
    implementation("androidx.media3:media3-ui:1.6.1")
}
