plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.sapiens.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.sapiens.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            pickFirsts += listOf(
                "META-INF/androidx.versionedparcelable_versionedparcelable.version",
                "META-INF/androidx.core_core.version",
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-svg:2.7.0")
    implementation("androidx.compose.material3:material3") {
        version {
            strictly("1.3.1")
        }
    }
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    // Firestore
    implementation("com.google.firebase:firebase-firestore-ktx")
    // Auth (구글 로그인)
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // 뉴스 제목·요약 등 한자어 → 한글 표기 (Firestore/북마크 로드 시 적용)
    implementation("kr.bydelta:koalanlp-core:2.1.4")
    // 동일 API(barteksc 기반). JitPack `com.github.barteksc:android-pdf-viewer`는 해석 실패가 많아 Maven Central 포크 사용.
    implementation("com.github.mhiew:android-pdf-viewer:3.2.0-beta.1") {
        exclude(group = "com.android.support")
    }
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

/** PR/푸시 전 로컬 검증: `./gradlew :app:verifyApp` (Lint 에러 시 실패) */
tasks.register("verifyApp") {
    group = "verification"
    description = "compileDebugKotlin + lintDebug — 푸시 전 실행 권장"
    dependsOn("lintDebug")
}