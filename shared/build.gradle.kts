plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
}

android {
  namespace = "com.example.shared"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    minSdk = 24
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  buildFeatures {
    compose = true
  }
}

dependencies {
  api(platform(libs.androidx.compose.bom))
  api(libs.androidx.activity.compose)
  api(libs.androidx.compose.material.icons.core)
  api(libs.androidx.compose.material.icons.extended)
  api(libs.androidx.compose.material3)
  api(libs.androidx.compose.ui)
  api(libs.androidx.compose.ui.graphics)
  api(libs.androidx.compose.ui.tooling.preview)
  api(libs.androidx.core.ktx)
  api(libs.androidx.datastore.preferences)
  api(libs.androidx.lifecycle.runtime.compose)
  api(libs.androidx.lifecycle.runtime.ktx)
  api(libs.androidx.lifecycle.viewmodel.compose)
  api(libs.androidx.navigation.compose)
  api(libs.androidx.room.ktx)
  api(libs.androidx.room.runtime)
  api(libs.coil.compose)
  api(libs.coil.video)
  api(libs.zxing.core)
  api(libs.androidx.media3.exoplayer)
  api(libs.androidx.media3.ui)
  api(libs.kotlinx.coroutines.android)
  api(libs.kotlinx.coroutines.core)
  "ksp"(libs.androidx.room.compiler)
}
