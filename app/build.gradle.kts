// `java`는 빌드 스크립트에서 Java 플러그인 확장을 가리키므로 java.util.Properties를 직접 쓸 수 없다.
import java.io.StringReader
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * release 서명 정보. 저장소에는 넣지 않는다(AGENTS.md 13).
 *
 * 읽는 곳은 두 군데뿐이다.
 * 1. 프로젝트 루트의 `keystore.properties` — `.gitignore` 대상이다.
 * 2. 환경 변수 `KEUNEY_KEYSTORE_FILE`·`KEUNEY_KEYSTORE_PASSWORD`·`KEUNEY_KEY_ALIAS`·`KEUNEY_KEY_PASSWORD`.
 *
 * 파일이 있으면 파일을 쓰고, 없으면 환경 변수를 본다.
 */
class ReleaseSigning(
    val storeFile: File,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
)

/**
 * 서명을 걸 수 있는 상태인지, 아니면 왜 못 거는지.
 *
 * 못 거는 이유를 문장으로 들고 다니는 까닭이 있다. 서명이 안 되면 그냥 서명 없는 APK가 나오므로,
 * 이유를 말해 주지 않으면 왜 서명이 빠졌는지 알 수 없다. 실제로 이 설정을 만들면서 파일 앞의 BOM
 * 때문에 조용히 서명되지 않는 것을 겪었다.
 */
sealed class SigningState {
    class Ready(val signing: ReleaseSigning) : SigningState()
    class Missing(val reason: String) : SigningState()
}

fun readReleaseSigning(propertiesFile: File, rootDir: File): SigningState {
    val source: String
    val values: Map<String, String?>
    if (propertiesFile.isFile) {
        source = propertiesFile.name
        // UTF-8로 읽고 BOM을 떼어 낸다. Properties.load(InputStream)은 ISO-8859-1로 읽으므로
        // 편집기가 붙인 BOM이 첫 키 이름에 섞여 들어가고, 값이 조용히 비게 된다.
        val text = propertiesFile.readText(Charsets.UTF_8).removePrefix("\uFEFF")
        val loaded = Properties().apply { load(StringReader(text)) }
        values = mapOf(
            "storeFile" to loaded.getProperty("storeFile"),
            "storePassword" to loaded.getProperty("storePassword"),
            "keyAlias" to loaded.getProperty("keyAlias"),
            "keyPassword" to loaded.getProperty("keyPassword"),
        )
    } else {
        source = "환경 변수"
        values = mapOf(
            "storeFile" to System.getenv("KEUNEY_KEYSTORE_FILE"),
            "storePassword" to System.getenv("KEUNEY_KEYSTORE_PASSWORD"),
            "keyAlias" to System.getenv("KEUNEY_KEY_ALIAS"),
            "keyPassword" to System.getenv("KEUNEY_KEY_PASSWORD"),
        )
    }
    val blank = values.filterValues { it.isNullOrBlank() }.keys
    if (blank.size == values.size) {
        return SigningState.Missing("서명 정보가 없다(keystore.properties도, 환경 변수도 없다)")
    }
    if (blank.isNotEmpty()) {
        return SigningState.Missing("$source 의 ${blank.joinToString(", ")} 값이 비어 있다")
    }
    // 상대 경로는 프로젝트 루트를 기준으로 본다. 키 파일 자체는 저장소 밖에 두는 것이 낫다.
    val store = File(values.getValue("storeFile")!!).let {
        if (it.isAbsolute) it else File(rootDir, it.path)
    }
    if (!store.isFile) {
        // 여기서 빌드를 실패시키지 않는다. 키가 없는 곳에서도 assembleRelease는 돌아야 한다.
        return SigningState.Missing(
            "$source 가 가리키는 키 파일이 없다: ${store.path} " +
                "(Windows 경로는 역슬래시가 이스케이프로 읽히므로 / 로 쓴다)",
        )
    }
    return SigningState.Ready(
        ReleaseSigning(
            storeFile = store,
            storePassword = values.getValue("storePassword")!!,
            keyAlias = values.getValue("keyAlias")!!,
            keyPassword = values.getValue("keyPassword")!!,
        ),
    )
}

val signingState = readReleaseSigning(rootProject.file("keystore.properties"), rootProject.projectDir)
val releaseSigning = (signingState as? SigningState.Ready)?.signing

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

    signingConfigs {
        if (releaseSigning != null) {
            create("release") {
                storeFile = releaseSigning.storeFile
                storePassword = releaseSigning.storePassword
                keyAlias = releaseSigning.keyAlias
                keyPassword = releaseSigning.keyPassword
            }
        }
    }

    buildTypes {
        release {
            // 서명 정보가 없으면 그대로 서명하지 않은 APK를 만든다. 빌드를 실패시키지 않는
            // 이유는 키가 없는 곳(CI, 남의 PC)에서도 test·lint·assembleRelease가 돌아야
            // 하기 때문이다. 서명 여부는 아래에서 한 번 알려 준다.
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

// release를 만들 때만 말한다. 다른 빌드에서 매번 찍으면 읽지 않게 된다.
if (gradle.startParameter.taskNames.any { it.contains("elease") }) {
    when (val state = signingState) {
        // 경로의 파일 이름만 알린다. 별칭·비밀번호는 찍지 않는다.
        is SigningState.Ready ->
            logger.lifecycle("[keuney] release 서명에 ${state.signing.storeFile.name}을 쓴다.")
        is SigningState.Missing ->
            logger.lifecycle(
                "[keuney] 서명하지 않은 release APK를 만든다: ${state.reason}. " +
                    "설정 방법은 README의 '릴리스 서명'을 본다.",
            )
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
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
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
    implementation(libs.media3.database)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    testImplementation(libs.junit)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.coroutines.test)

    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.junit)
    kspAndroidTest(libs.hilt.compiler)
}
