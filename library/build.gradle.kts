import com.vanniktech.maven.publish.SonatypeHost
import java.util.Properties

object This {
    const val longName = "Concurrent selector - Angelos Project™"
    const val longDescription = "Asynchronous selector"
    const val url = "https://github.com/angelos-project/angelos-project-selector"
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kover)
}

kotlin {
    explicitApi()
    jvmToolchain(libs.versions.jvm.toolchain.get().toInt())

    jvm()
    js {
        browser()
        nodejs()
    }
    // Wasm
    /*wasmJs {
        browser()
        nodejs()
    }
    wasmWasi { nodejs() }*/
    // Android
    androidTarget {
        /*@OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }*/
        publishLibraryVariants("release")
    }
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX64()
    androidNativeX86()
    // Linux
    linuxArm64()
    linuxX64()
    // macOS
    macosArm64()
    macosX64()
    // MingW
    mingwX64()
    // iOS
    iosArm64()
    iosX64()
    iosSimulatorArm64()
    // tvOS
    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()
    // watchOS
    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlin.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.kotlin.coroutines.android)
        }
        jvmMain.dependencies {
            implementation(libs.kotlin.mockito)
        }
    }
}

android {
    namespace = group.toString()
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        /*sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17*/
    }
}


mavenPublishing {
    //publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    //signAllPublications()

    /**
     * The temporary artifact setup, final is coming later at some point.
     * DO NOT USE FOR SONATYPE NEXUS
     * */
    coordinates(group.toString(), rootProject.name, version.toString())

    publishing {
        repositories {
            maven {
                name = "Repsy"
                val localProps = Properties()
                val localPropsFile = file("${rootProject.projectDir.path}/local.properties")
                if (localPropsFile.exists()) {
                    localProps.load(localPropsFile.inputStream())
                }
                val repsyUsername = localProps.getProperty("repsy.username") ?: System.getenv("REPSY_USERNAME") ?: ""
                val repsyPassword = localProps.getProperty("repsy.password") ?: System.getenv("REPSY_PASSWORD") ?: ""
                credentials {
                    username = repsyUsername
                    password = repsyPassword
                }
                url = uri("https://repo.repsy.io/$repsyUsername/angelos-project")
            }
        }
    }

    pom {
        name.set(This.longName)
        description.set(This.longDescription)
        inceptionYear.set("2025")
        url.set(This.url)

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                name.set("Kristoffer Paulsson")
                email.set("kristoffer.paulsson@talenten.se")
                url.set("https://github.com/kristoffer-paulsson")
            }
        }
        scm {
            url.set(This.url)
            connection.set("scm:git:git://github.com/angelos-project/angelos-project-selector.git")
            developerConnection.set("scm:git:ssh://github.com:angelos-project/angelos-project-selector.git")
        }
    }
}

dokka {
    dokkaPublications.html {
        moduleName.set(rootProject.name)
    }

    pluginsConfiguration.html {
        footerMessage.set("Copyright (c) 2025-2026 Kristoffer Paulsson.")
    }

    dokkaSourceSets.commonMain {
        sourceLink {
            remoteUrl(This.url + "/tree/master/library")
        }
    }
}

kover {
    reports {
        total {
            xml.onCheck.set(true)
            html.onCheck.set(true)
        }
    }
}