plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
    id("jacoco")
}

group = "com.github.IrynaTsymbaliuk"
version = "1.2.2"

android {
    namespace = "com.tracker.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 21

        version = project.version.toString()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField("String", "LIBRARY_VERSION", "\"${project.version}\"")
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.health.connect)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.withType<Test>().configureEach {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

val jacocoExcludes = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
)

fun coverageClassDirs() =
    fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug").get().asFile) {
        exclude(jacocoExcludes)
    }

fun coverageExecData() =
    fileTree(layout.buildDirectory.get().asFile) {
        include("**/testDebugUnitTest.exec")
    }

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    group = "verification"
    description = "Generates a JaCoCo coverage report from the debug unit tests."

    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(coverageClassDirs())
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(coverageExecData())
}

tasks.register<JacocoCoverageVerification>("jacocoCoverageVerification") {
    dependsOn("testDebugUnitTest")
    group = "verification"
    description = "Fails the build if line coverage drops below 80%."

    classDirectories.setFrom(coverageClassDirs())
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(coverageExecData())

    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

// Maven publishing configuration for JitPack
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = project.group.toString()
                artifactId = "tracker"
                version = project.version.toString()

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
