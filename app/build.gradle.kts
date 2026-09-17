import org.gradle.api.tasks.testing.Test
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val versionPropertiesFile = rootProject.file("version.properties")
require(versionPropertiesFile.isFile) {
    "Missing required version file: ${versionPropertiesFile.absolutePath}"
}

val versionProperties = Properties()
versionPropertiesFile.inputStream().use(versionProperties::load)

val expectedVersionKeys = setOf("VERSION_NAME", "VERSION_CODE")
val actualVersionKeys = versionProperties.stringPropertyNames()
require(actualVersionKeys == expectedVersionKeys) {
    "version.properties must contain exactly VERSION_NAME and VERSION_CODE"
}

fun requiredVersionProperty(key: String): String {
    val value = versionProperties.getProperty(key)?.trim()
    require(!value.isNullOrEmpty()) {
        "version.properties property $key must not be empty"
    }
    return value
}

val versionNameFromProperties = requiredVersionProperty("VERSION_NAME")
require(Regex("^[0-9]+\\.[0-9]+\\.[0-9]+$").matches(versionNameFromProperties)) {
    "VERSION_NAME must use X.Y.Z format"
}

val versionCodeFromProperties = requiredVersionProperty("VERSION_CODE")
require(Regex("^[1-9][0-9]*$").matches(versionCodeFromProperties)) {
    "VERSION_CODE must be a positive integer without leading zeroes"
}
val parsedVersionCode = versionCodeFromProperties.toIntOrNull()
require(parsedVersionCode != null) {
    "VERSION_CODE is too large for Android versionCode: $versionCodeFromProperties"
}

android {
    namespace = "tw.mustp.booxcoversync"
    compileSdk = 36
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "tw.mustp.booxcoversync"
        minSdk = 26
        targetSdk = 35
        versionCode = parsedVersionCode
        versionName = versionNameFromProperties

        testInstrumentationRunner = "android.test.InstrumentationTestRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

tasks.withType<Test>().configureEach {
    val testRuntimeHome = rootProject.layout.projectDirectory.dir(".gradle-user-home").asFile
    val testRuntimeTemp = testRuntimeHome.resolve("tmp")
    testRuntimeTemp.mkdirs()
    systemProperty(
        "user.home",
        testRuntimeHome.absolutePath,
    )
    systemProperty("java.io.tmpdir", testRuntimeTemp.absolutePath)
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
}
