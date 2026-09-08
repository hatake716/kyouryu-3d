import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val signingFile = rootProject.file("keystore.properties")
val signing = Properties().apply { if (signingFile.exists()) signingFile.inputStream().use { load(it) } }
android {
    namespace = "io.github.hatake716.dinobones"
    compileSdk = 36
    defaultConfig {
        applicationId = "io.github.hatake716.dinobones"
        minSdk = 26
        targetSdk = 36
        versionCode = 12
        versionName = "1.10.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    if (signingFile.exists()) signingConfigs.create("release") {
        storeFile = rootProject.file(signing.getProperty("storeFile"))
        storePassword = signing.getProperty("storePassword")
        keyAlias = signing.getProperty("keyAlias")
        keyPassword = signing.getProperty("keyPassword")
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (signingFile.exists()) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
}
tasks.register<JavaExec>("exportTheaterSources") {
    dependsOn("compileDebugUnitTestKotlin")
    classpath=files("build/tmp/kotlin-classes/debugUnitTest","build/tmp/kotlin-classes/debug")+configurations.getByName("debugUnitTestRuntimeClasspath")
    mainClass.set("io.github.hatake716.dinobones.tools.ExportTheater")
    args(rootProject.file("artifacts/theater-1.6/sources").absolutePath)
    maxHeapSize="2g"
}
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
