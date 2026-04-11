plugins {
    alias(libs.plugins.android.application)
}

android {
    signingConfigs {
        create("release") {
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
            storeFile = file("${rootDir}/keystore.jks")
        }
    }
    namespace = "com.vikas.gtr2e"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vikas.gtr2e"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.1.5"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }

}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.circularprogressbar)
    implementation(libs.cardview)
    implementation(libs.retrofit)
    implementation(libs.converterGson)
    implementation(libs.loggingInterceptor)
    implementation(libs.fragment)
    implementation(libs.preference)
    implementation(libs.jacksonDatabind)
    implementation(libs.jacksonAnnotations)
    implementation(libs.navigationUI)
    implementation(libs.navigationFragment)
    implementation(libs.vico.core)
    implementation(libs.vico.views)
    implementation(libs.aaChart)
    implementation(libs.glide)
    annotationProcessor(libs.glide)
    implementation(libs.room)
    annotationProcessor(libs.roomAnnotationProcessor)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}