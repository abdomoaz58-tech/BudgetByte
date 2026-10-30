plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.budgetbyte"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.budgetbyte.v2"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    // تفعيل ViewBinding
    buildFeatures {
        viewBinding = true
    }

    // تقسيم المعماريات لتخفيض حجم الـ APK ومنع حزم الـ 41 ميجابايت من ML Kit
    splits {
        abi {
            isEnable = true
            isUniversalApk = false
            reset()
            include("arm64-v8a", "armeabi-v7a")
        }
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // ── الأساسيات ──
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // ── مكتبات الواجهة (UI) ──
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // ── Navigation Component (التنقل بين الفراجمينتس) ──
    implementation("androidx.navigation:navigation-fragment:2.8.5")
    implementation("androidx.navigation:navigation-ui:2.8.5")

    // ── Room Database (قاعدة البيانات) ──
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // ── MPAndroidChart (الرسوم البيانية) ──
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // ── الحماية والبصمة (Security & Biometrics) ──
    implementation("androidx.security:security-crypto:1.1.0-alpha06") 
    implementation("androidx.biometric:biometric:1.1.0")
    
    // ── ML Kit (مسح الفواتير بالذكاء الاصطناعي) ──
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.1")
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0")
}
