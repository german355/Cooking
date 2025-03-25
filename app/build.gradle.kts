plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.cooking"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.cooking"
        minSdk = 34
        targetSdk = 34
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
    
    // Добавляем поддержку загрузки шрифтов из Google Fonts
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation(libs.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("io.socket:socket.io-client:2.0.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    
    // Добавляем зависимость Google Fonts для загрузки шрифтов
    implementation("androidx.core:core:1.12.0")

    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-scalars:2.9.0")
    // Конвертер Gson для обработки JSON
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    // Gson для работы с JSON
    implementation ("com.google.code.gson:gson:2.9.0")

    implementation("com.github.bumptech.glide:glide:4.16.0")
}