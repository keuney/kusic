plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.keuney.music"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "com.keuney.music"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "com.keuney.music.HiltTestRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

tasks.withType<Test>().configureEach {
    if (name != "sourceContractTest") exclude("**/*SourceContractTest.class")
}

tasks.register<Test>("sourceContractTest") {
    group = "verification"
    description = "Runs explicit live source checks; excluded from normal unit tests."
    val unitTests = tasks.named<Test>("testDebugUnitTest")
    dependsOn(unitTests)
    testClassesDirs = files(unitTests.map { it.testClassesDirs })
    classpath = files(unitTests.map { it.classpath })
    include("**/*SourceContractTest.class")
    if (providers.gradleProperty("sourceContractUseWindowsTrust").orNull == "true") {
        check(System.getProperty("os.name").startsWith("Windows"))
        systemProperty("javax.net.ssl.trustStoreType", "Windows-ROOT")
        systemProperty("javax.net.ssl.trustStoreProvider", "SunMSCAPI")
        systemProperty("javax.net.ssl.trustStore", "NONE")
    }
    outputs.upToDateWhen { false }
    outputs.doNotCacheIf("Live provider response must be checked again") { true }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.material3)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences.core)
    implementation(libs.coroutines.core)
    implementation(libs.datastore.core.okio)
    implementation(libs.okio)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime.compose)

    testImplementation(libs.junit)
    testImplementation(libs.ktor.client.mock)

    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.junit)
    kspAndroidTest(libs.hilt.compiler)
}
