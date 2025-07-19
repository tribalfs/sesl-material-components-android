import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.tasks.GenerateBuildConfig
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties
import java.util.regex.Pattern

buildscript {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.7.3")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.20")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:2.0.0")
    }
}


fun String.toEnvVarStyle(): String =
    this.replace(Regex("([a-z])([A-Z])"), "$1_$2")
        .uppercase()


fun getGithubProperty(key: String): String {
    val githubProperties = Properties().apply {
        val file = rootProject.file("github.properties")
        if (file.exists()) {
            file.inputStream().use { load(it) }
        }
    }
    return githubProperties.getProperty(key)
        ?: rootProject.findProperty(key)?.toString()
        ?: System.getenv(key.toEnvVarStyle())
        ?: throw GradleException("GitHub $key not found")
}


extra["ghUsername"] = getGithubProperty("ghUsername")
extra["ghAccessToken"] = getGithubProperty("ghAccessToken")
extra["mdcLibraryVersion"] = "1.12.0+1.0.31-sesl8+rev1"
extra["mdcLibraryPackage"] = "com.google.android.material"

allprojects {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://maven.pkg.github.com/tribalfs/sesl-androidx")
            credentials {
                username = rootProject.extra["ghUsername"] as String
                password = rootProject.extra["ghAccessToken"] as String
            }
        }
    }
}


fun getArchivesBaseName(name: String): String {
    if (name == "lib") return "material"

    val pathComponents = name.split("-")
    val knownComponents = listOf("lib", "java", "com", "google", "android", "material")

    var firstUnknownComponent = knownComponents.size
    for (i in knownComponents.indices) {
        if (i >= pathComponents.size) break
        if (pathComponents[i] != knownComponents[i]) {
            firstUnknownComponent = i
            break
        }
    }

    var result = "material"
    for (i in firstUnknownComponent until pathComponents.size) {
        result += "-" + pathComponents[i]
    }
    return result
}

subprojects {
    tasks.withType<Test> {
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
        forkEvery = 80
        maxHeapSize = "2048m"
        minHeapSize = "1024m"
    }
}

subprojects {
    val mdcLibraryPackage: String by rootProject.extra
    val mdcLibraryVersion: String by rootProject.extra

    version = mdcLibraryVersion
    group = "sesl.$mdcLibraryPackage"

    if (project.name.contains("tests")) {
        project.configurations.all {
            dependencyConstraints.configureEach { version { strictly("") } }
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions.jvmTarget.set(JvmTarget.fromTarget("1.8"))
    }


    plugins.whenPluginAdded {
        val isAndroidLibrary = javaClass.name == "com.android.build.gradle.LibraryPlugin"
        val isAndroidApp = javaClass.name == "com.android.build.gradle.AppPlugin"
        val isAndroidTest = javaClass.name == "com.android.build.gradle.TestPlugin"

        if (isAndroidLibrary || isAndroidApp) {
            extensions.configure<BaseExtension>("android") {
                // Enable code coverage for debug builds only if we are not running inside the IDE,
                // since enabling coverage reports breaks the method parameter resolution in the IDE
                // debugger. Note that we avoid doing this for Android Test projects as it causes
                // crashes on Dalvik ('Class ref in pre-verified class resolved to unexpected implementation')
                buildTypes.getByName("debug").isTestCoverageEnabled =
                    !hasProperty("android.injected.invoked.from.ide")
            }
        }

        if (isAndroidLibrary || isAndroidApp || isAndroidTest) {
            extensions.configure<BaseExtension>("android") {
                compileSdkVersion(35)
                defaultConfig {
                    minSdk = 21
                    targetSdk = 35
                    vectorDrawables.useSupportLibrary = true
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_1_8
                    targetCompatibility = JavaVersion.VERSION_1_8
                }

            }

            if (isAndroidLibrary) {
                extensions.findByType<LibraryExtension>()?.apply {
                    lint.checkOnly += "NewApi"

                    namespace = mdcLibraryPackage

                    sourceSets["main"].resources.srcDir(layout.buildDirectory.file("javaResources"))

                    publishing {
                        singleVariant("release") {
                            withSourcesJar()
                            withJavadocJar()
                        }
                    }
                }

                afterEvaluate {
                    tasks.register("writeVersionFile") {
                        val versionFileName =
                            "${mdcLibraryPackage}_${getArchivesBaseName(project.name)}.version"
                        val versionFileDir =
                            file(layout.buildDirectory.file("javaResources/META-INF"))
                        versionFileDir.mkdirs()
                        val versionFile = File(versionFileDir, versionFileName)
                        versionFile.writeText("${mdcLibraryVersion}\n")
                    }

                    extensions.configure<LibraryExtension>("android") {
                        libraryVariants.all {
                            processJavaResourcesProvider.get().dependsOn(tasks["writeVersionFile"])
                        }
                    }

                    tasks.register("updateVersionBadge") {
                        fun String.escaped(): String = replace(".", "\\.").replace("-", "--").replace("+", "%2B")
                        val readmeFile = file("${rootProject.projectDir}/README.md")
                        val readmeContent = readmeFile.readText()
                        val baseUrl = "https://img.shields.io/badge/sesl.$mdcLibraryPackage:material"
                        val escapedVersion = mdcLibraryVersion.escaped()
                        val badgeUrl = "$baseUrl-$escapedVersion-blue?logo=GitHub"
                        val pattern = Pattern.compile("${baseUrl.escaped()}-\\d+.*blue\\?logo=GitHub")
                        val updatedContent = pattern.matcher(readmeContent).replaceFirst(badgeUrl)
                        readmeFile.writeText(updatedContent)
                    }
                }
            }
        }

        if (isAndroidLibrary) {
            afterEvaluate { tasks.withType<GenerateBuildConfig> { enabled = false } }
        }
    }
}
