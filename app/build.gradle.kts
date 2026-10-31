plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
  id("org.jetbrains.kotlin.plugin.serialization")
}

fun launchSetting(name: String, fallback: String = ""): String =
  providers.gradleProperty(name).orElse(providers.environmentVariable(name)).getOrElse(fallback)
fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
  namespace = "com.sammyi57.pdftools"
  compileSdk = 36
  compileSdkExtension = 19 // Jetpack PDF 1.0.0-alpha18 builds against the PDF APIs added in SDK extension 19

  defaultConfig {
    // Same id and storage layout as the Expo app, so this build installs over it and keeps its files, scans and Drive link.
    applicationId = "com.waseem.pdfpocket"
    minSdk = 24
    targetSdk = 36
    versionCode = 3
    versionName = "2.1.0"
    buildConfigField("String", "REVENUECAT_PUBLIC_KEY", quoted(launchSetting("REVENUECAT_PUBLIC_KEY")))
    buildConfigField("String", "PREMIUM_ENTITLEMENT", quoted("premium"))
    buildConfigField("String", "SUPPORT_EMAIL", quoted(launchSetting("SUPPORT_EMAIL")))
    buildConfigField("String", "WEBSITE_URL", quoted(launchSetting("WEBSITE_URL")))
    manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
    // OpenCV ships a native lib per ABI; one keeps the APK small. `-PallAbis` (the release workflow) builds an APK per
    // ABI plus a universal one instead.
    if (!project.hasProperty("allAbis")) ndk { abiFilters += "arm64-v8a" }
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    // The Expo build's debug key, so a debug build replaces the installed app instead of failing on a signature mismatch.
    getByName("debug") {
      if (file("debug.keystore").exists()) storeFile = file("debug.keystore")
    }
    // Your upload key, kept out of the repo in ~/.gradle/gradle.properties (same properties the Expo build read).
    val store = findProperty("PDF_TOOLS_UPLOAD_STORE_FILE") as String?
    if (store != null) create("release") {
      storeFile = file(store)
      storePassword = findProperty("PDF_TOOLS_UPLOAD_STORE_PASSWORD") as String?
      keyAlias = findProperty("PDF_TOOLS_UPLOAD_KEY_ALIAS") as String?
      keyPassword = findProperty("PDF_TOOLS_UPLOAD_KEY_PASSWORD") as String?
    }
  }

  buildTypes {
    // `-PsideBySide` installs the debug build (and its on-device tests) next to the real app instead of over it, so a
    // test run never touches its files: ./gradlew :app:connectedDebugAndroidTest -PsideBySide
    debug {
      applicationIdSuffix = ".debug"
      buildConfigField("boolean", "ADS_ENABLED", (!project.hasProperty("storeScreenshots")).toString())
      buildConfigField("String", "BANNER_AD_UNIT_ID", quoted("ca-app-pub-3940256099942544/6300978111"))
      if (project.hasProperty("sideBySide")) applicationIdSuffix = ".test"
    }
    release {
      buildConfigField("boolean", "ADS_ENABLED", launchSetting("LIVE_ADS_ENABLED", "false").toBoolean().toString())
      buildConfigField("String", "BANNER_AD_UNIT_ID", quoted(launchSetting("ADMOB_BANNER_ID")))
      manifestPlaceholders["admobAppId"] = launchSetting("ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
    }
  }

  splits {
    abi {
      isEnable = project.hasProperty("allAbis")
      reset()
      include("arm64-v8a", "armeabi-v7a", "x86_64")
      isUniversalApk = true
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures { compose = true; buildConfig = true }
  packaging { jniLibs { useLegacyPackaging = true } }
}

kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    optIn.addAll(
      "androidx.compose.animation.ExperimentalSharedTransitionApi",
      "androidx.compose.foundation.ExperimentalFoundationApi",
      "androidx.compose.foundation.layout.ExperimentalLayoutApi",
      "androidx.compose.material3.ExperimentalMaterial3Api",
    )
  }
}

dependencies {
  implementation("com.google.android.gms:play-services-ads:25.5.0")
  implementation("com.google.android.ump:user-messaging-platform:4.0.0")
  implementation("com.revenuecat.purchases:purchases:10.15.1")
  implementation(platform("androidx.compose:compose-bom:2026.06.01"))
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.foundation:foundation")
  implementation("androidx.compose.animation:animation")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.activity:activity-compose:1.11.0")
  implementation("androidx.navigation:navigation-compose:2.9.4")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
  implementation("dev.chrisbanes.haze:haze:1.7.0") // frosted-glass blur behind the tab bar (Compose has no backdrop blur); 1.7.3 needs compileSdk 37
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

  // Scanner: camera, page detection and filters (OpenCV + the bundled DocAligner model), QR codes.
  val camerax = "1.6.0"
  implementation("androidx.camera:camera-core:$camerax")
  implementation("androidx.camera:camera-camera2:$camerax")
  implementation("androidx.camera:camera-lifecycle:$camerax")
  implementation("androidx.camera:camera-view:$camerax")
  implementation("org.opencv:opencv:4.12.0")
  implementation("com.google.zxing:core:3.5.3")

  // PDF engine (Apache PDFBox ported to Android) and on-device OCR with the bundled Latin model.
  implementation("com.tom-roush:pdfbox-android:2.0.27.0")
  implementation("com.google.mlkit:text-recognition:16.0.1")

  // The in-app viewer: Jetpack's PDF viewer (the Drive viewer's engine) with text selection, search, zoom and links.
  // It needs Android 12 with the PDF system update; older phones keep opening PDFs in their own viewer.
  implementation("androidx.pdf:pdf-compose:1.0.0-alpha18")
  implementation("androidx.pdf:pdf-document-service:1.0.0-alpha18")

  // Google Drive sync: Identity AuthorizationClient for a drive.file token.
  implementation("com.google.android.gms:play-services-auth:22.0.0")

  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test:runner:1.7.0")
  androidTestImplementation("androidx.test.ext:junit:1.3.0")
  androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}

// Verification builds may be unsigned/test-only. Store publishing must use this explicit task.
tasks.register("validateStoreConfiguration") {
  doLast {
    require(launchSetting("SUPPORT_EMAIL").contains("@")) { "Set SUPPORT_EMAIL before publishing." }
    require(launchSetting("WEBSITE_URL").startsWith("https://")) { "Set the public WEBSITE_URL before publishing." }
    require(launchSetting("REVENUECAT_PUBLIC_KEY").startsWith("goog_")) { "Set the public Google Play RevenueCat key." }
    require(findProperty("PDF_TOOLS_UPLOAD_STORE_FILE") != null) { "Configure your own upload signing key." }
    require(launchSetting("LIVE_ADS_ENABLED", "false").toBoolean()) { "The current Premium offer removes ads. Activate production ads before selling this offer." }
    if (launchSetting("LIVE_ADS_ENABLED", "false").toBoolean()) {
      require(launchSetting("ADMOB_APP_ID").startsWith("ca-app-pub-") && !launchSetting("ADMOB_APP_ID").contains("3940256099942544")) { "Set your own AdMob app ID." }
      require(launchSetting("ADMOB_BANNER_ID").startsWith("ca-app-pub-") && !launchSetting("ADMOB_BANNER_ID").contains("3940256099942544")) { "Set your own banner ID." }
    }
  }
}
tasks.register("storeBundle") {
  dependsOn("validateStoreConfiguration", "bundleRelease")
}
