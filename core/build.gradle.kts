plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.tracker.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 21

        version = "4.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField("String", "LIBRARY_VERSION", "\"4.0.0\"")
        buildConfigField("String", "LIBRARY_NAME", "\"Tracker\"")
    }

    buildFeatures {
        buildConfig = true
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    // AndroidX Core
    implementation(libs.androidx.core.ktx)

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Maven publishing configuration for JitPack
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.github.IrynaTsymbaliuk"
                artifactId = "tracker"
                version = "4.0.0"

                pom {
                    name.set("Tracker")
                    description.set("Android library for automatic habit tracking through device usage analysis")
                    url.set("https://github.com/IrynaTsymbaliuk/Tracker")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("IrynaTsymbaliuk")
                            name.set("Iryna Tsymbaliuk")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/IrynaTsymbaliuk/Tracker.git")
                        developerConnection.set("scm:git:ssh://github.com/IrynaTsymbaliuk/Tracker.git")
                        url.set("https://github.com/IrynaTsymbaliuk/Tracker")
                    }
                }
            }
        }
    }
}