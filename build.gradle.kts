plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
}

group = "org.connecttag.lib"
version = "1.0.2"

kotlin {
    jvmToolchain(21)

    android {
        namespace = "org.connecttag.lib.gis"
        compileSdk = 36
        minSdk = 24
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.serialization.json)
            api(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

publishing {
    publications.withType<MavenPublication> {
        pom {
            name.set("ConnectTag Geo-GIS Kit (KMP)")
            description.set("Kotlin Multiplatform suite for Geodesic spatial math, clustering, and GeoJSON/KML/GPX/WKT GIS format parsing and writing.")
            url.set("https://github.com/connecttag/geo-gis-kit-kmp")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("connecttagye")
                    name.set("Connect Tag")
                    email.set("connecttagye@gmail.com")
                }
            }
            scm {
                connection.set("scm:git:https://github.com/connecttag/geo-gis-kit-kmp.git")
                developerConnection.set("scm:git:ssh://git@github.com:connecttag/geo-gis-kit-kmp.git")
                url.set("https://github.com/connecttag/geo-gis-kit-kmp/tree/main")
            }
        }
    }
}
