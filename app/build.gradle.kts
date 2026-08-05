plugins {
    alias(libs.plugins.android.application)
//    alias(libs.plugins.google.services) // todo live
    alias(libs.plugins.crashlytics)
}

android {
    namespace = "com.facechanger.faceswap.enhance"
    compileSdk = 36

    defaultConfig {
//        applicationId = "com.facechanger.faceswap.enhance" // todo live
        applicationId = "com.dsfsdf.werwer.dfgdfg" // todo remove


        minSdk = 26
        targetSdk = 36


//        versionCode = 8 // todo live
//        versionName = "1.1" // todo live

        versionCode = 1 // todo remove
        versionName = "0.1" // todo remove

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//        manifestPlaceholders["izooto_app_id"] = "66dd8d127ebc7730cf309b301599948cac248049" // todo live
        manifestPlaceholders["izooto_app_id"] = "" // todo remove
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
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
    implementation("com.google.android.gms:play-services-ads:25.4.0")
    implementation("com.tencent:mmkv-static:1.3.16")

    implementation("com.google.code.gson:gson:2.14.0")
    implementation("com.intuit.sdp:sdp-android:1.1.1")
    implementation("androidx.lifecycle:lifecycle-process:2.11.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.11.0")
    implementation("com.getkeepsafe.relinker:relinker:1.4.5")

    implementation("com.android.installreferrer:installreferrer:2.2")

    implementation(platform("com.google.firebase:firebase-bom:34.17.0"))
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
    implementation("com.airbnb.android:lottie:6.7.1")
    implementation("com.revenuecat.purchases:purchases:10.16.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("com.facebook.android:facebook-android-sdk:18.3.0")
    implementation(libs.androidx.core.splashscreen)
    implementation("com.google.firebase:firebase-messaging")
    implementation("androidx.work:work-runtime:2.11.2")

    // Retrofit + OkHttp (network layer)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Media3 ExoPlayer (in-app video playback)
    implementation("androidx.media3:media3-exoplayer:1.10.1")
    implementation("androidx.media3:media3-ui:1.6.1")


// iXooto
    implementation("com.izooto:android-sdk:3.5.3")
    implementation("androidx.work:work-runtime:2.11.2")
    implementation("androidx.browser:browser:1.10.0")

    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")

}
