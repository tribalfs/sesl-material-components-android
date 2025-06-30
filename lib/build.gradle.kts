import com.android.build.gradle.internal.publishing.AndroidArtifacts.ARTIFACT_TYPE
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.testImplementation

plugins {
    id("com.android.library")
    id("maven-publish")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.dokka")
}

dependencies {
    api("androidx.activity:activity:1.8.0")
    api("androidx.annotation:annotation:1.2.0")
    api("androidx.cardview:cardview:1.0.0")
    api("androidx.constraintlayout:constraintlayout:2.0.1")
    api("androidx.dynamicanimation:dynamicanimation:1.0.0")
    api("androidx.annotation:annotation-experimental:1.0.0")
    api("androidx.lifecycle:lifecycle-runtime:2.0.0")
    api("androidx.transition:transition:1.5.0")
    api("androidx.recyclerview:recyclerview-selection:1.0.0")
    api("androidx.vectordrawable:vectordrawable:1.1.0")
    api("androidx.resourceinspection:resourceinspection-annotation:1.0.1")
    annotationProcessor("androidx.resourceinspection:resourceinspection-processor:1.0.1")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.0.21"))
    implementation("com.google.errorprone:error_prone_annotations:2.15.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.21")

    api("sesl.androidx.core:core:1.16.0+1.0.7-sesl8+rev0")
    implementation("sesl.androidx.core:core-ktx:1.16.0+1.0.0-sesl8+rev0")
    api("sesl.androidx.appcompat:appcompat:1.7.1+1.0.18-sesl8+rev0")
    api("sesl.androidx.coordinatorlayout:coordinatorlayout:1.3.0+1.0.0-sesl8+rev0")
    api("sesl.androidx.drawerlayout:drawerlayout:1.2.0+1.0.0-sesl8+rev0")
    api("sesl.androidx.fragment:fragment:1.8.8+1.0.5-sesl8+rev0")
    api("sesl.androidx.recyclerview:recyclerview:1.4.0+1.0.12-sesl8+rev0")
    api("sesl.androidx.viewpager2:viewpager2:1.1.0+1.0.0-sesl8+rev0")


    testImplementation("androidx.test:core:1.4.0")
    testImplementation("androidx.test:runner:1.4.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.truth:truth:0.45")
    testImplementation("org.mockito:mockito-core:2.25.0")
    testImplementation("org.robolectric:robolectric:4.9")
}

val mdcLibraryDirectory = "com/google/android/material"
val srcDirs = listOf(
    "$mdcLibraryDirectory/animation",
    "$mdcLibraryDirectory/appbar",
    "$mdcLibraryDirectory/badge",
    "$mdcLibraryDirectory/behavior",
    "$mdcLibraryDirectory/bottomappbar",
    "$mdcLibraryDirectory/bottomnavigation",
    "$mdcLibraryDirectory/bottomsheet",
    "$mdcLibraryDirectory/button",
    "$mdcLibraryDirectory/canvas",
    "$mdcLibraryDirectory/card",
    "$mdcLibraryDirectory/carousel",
    "$mdcLibraryDirectory/checkbox",
    "$mdcLibraryDirectory/chip",
    "$mdcLibraryDirectory/circularreveal",
    "$mdcLibraryDirectory/circularreveal/cardview",
    "$mdcLibraryDirectory/circularreveal/coordinatorlayout",
    "$mdcLibraryDirectory/color",
    "$mdcLibraryDirectory/datepicker",
    "$mdcLibraryDirectory/dialog",
    "$mdcLibraryDirectory/divider",
    "$mdcLibraryDirectory/drawable",
    "$mdcLibraryDirectory/elevation",
    "$mdcLibraryDirectory/expandable",
    "$mdcLibraryDirectory/floatingactionbutton",
    "$mdcLibraryDirectory/imageview",
    "$mdcLibraryDirectory/internal",
    "$mdcLibraryDirectory/materialswitch",
    "$mdcLibraryDirectory/math",
    "$mdcLibraryDirectory/menu",
    "$mdcLibraryDirectory/motion",
    "$mdcLibraryDirectory/navigation",
    "$mdcLibraryDirectory/navigationrail",
    "$mdcLibraryDirectory/progressindicator",
    "$mdcLibraryDirectory/radiobutton",
    "$mdcLibraryDirectory/resources",
    "$mdcLibraryDirectory/ripple",
    "$mdcLibraryDirectory/search",
    "$mdcLibraryDirectory/shape",
    "$mdcLibraryDirectory/shadow",
    "$mdcLibraryDirectory/sidesheet",
    "$mdcLibraryDirectory/slider",
    "$mdcLibraryDirectory/snackbar",
    "$mdcLibraryDirectory/stateful",
    "$mdcLibraryDirectory/switchmaterial",
    "$mdcLibraryDirectory/tabs",
    "$mdcLibraryDirectory/textfield",
    "$mdcLibraryDirectory/textview",
    "$mdcLibraryDirectory/theme",
    "$mdcLibraryDirectory/theme/overlay",
    "$mdcLibraryDirectory/timepicker",
    "$mdcLibraryDirectory/tooltip",
    "$mdcLibraryDirectory/transition",
    "$mdcLibraryDirectory/transformation",
    "$mdcLibraryDirectory/typography",
    "$mdcLibraryDirectory/lists",
    "$mdcLibraryDirectory/materialswitch",
    "$mdcLibraryDirectory/oneui"
)

android {
    sourceSets["main"].apply {
        manifest.srcFile("java/$mdcLibraryDirectory/AndroidManifest.xml")
        java.srcDir("java")
        java.include(*srcDirs.map { "$it/**/*.java" }.toTypedArray())
        java.exclude("**/build/**")
        srcDirs.forEach {
            res.srcDirs("java/$it/res", "java/$it/res-public")
        }
    }

    sourceSets["test"].apply {
        java.srcDir("javatests")
        srcDirs.forEach { res.srcDir("javatests/$it/res") }
    }

    testOptions.unitTests.isIncludeAndroidResources = true

    buildTypes.all {
        consumerProguardFiles("proguard-behaviors.pro", "proguard-inflater.pro")
    }
}


tasks.register<Javadoc>("generateJavadocs") {
    if (project.hasProperty("online")) {
        (options as? StandardJavadocDocletOptions)?.apply {
            addStringOption("toroot", "/")
            addStringOption("hdf", "android.whichdoc online")
            addStringOption("hdf", "dac")
            addBooleanOption("devsite", true)
            addBooleanOption("yamlV2", true)
            addStringOption("dac_libraryroot", "com/google/android/material")
            addStringOption("dac_dataname", "MATERIAL_DATA")
        }
    }

    if (project.hasProperty("docletPathRoot")) {
        val docletPathRoot = project.property("docletPathRoot") as String
        val outputPath = project.findProperty("outputPath") as? String ?: "doclava-out"
        val javaSources = android.sourceSets["main"].java.srcDirs
        this@register.source = fileTree(javaSources) { include("**/*.java") }
        title = null
        options.destinationDirectory = (File(outputPath))
        classpath = files("${android.sdkDirectory}/platforms/${android.compileSdkVersion}/android.jar")
        (options as? StandardJavadocDocletOptions)?.apply {
            addStringOption("federate Android", "https://developer.android.com")
            encoding = "UTF-8"
            doclet = "com.google.doclava.Doclava"
            docletpath = listOf(
                file(project.property("doclavaJar") as String),
                file("$docletPathRoot/jsilver/v1_0_0/jsilver.jar")
            )
        }
    }
}

tasks.register<Javadoc>("generateApiXml") {
    if (project.hasProperty("apiName")) {
        val jdiff = project.property("jdiffJar") as String
        val apiName = project.property("apiName") as String
        val javaSources = android.sourceSets["main"].java.srcDirs
        source = fileTree(javaSources) { include("**/*.java") }
        classpath =
            files("${android.sdkDirectory}/platforms/${android.compileSdkVersion}/android.jar")
        options.doclet = "jdiff.JDiff"
        (options as? StandardJavadocDocletOptions)?.apply {
            addStringOption("subpackages", ".")
            addStringOption("apiname", apiName)
            docletpath = listOf(file(jdiff))
            // Doclava does not understand -notimestamp option that is default since Gradle 6.0
            isNoTimestamp = false
        }
    }

    doLast {
        val apiName = project.findProperty("apiName") as? String ?: return@doLast
        val xmlFile = "lib/$apiName.xml"
        if (OperatingSystem.current().isLinux) {
            exec { commandLine("sed", "-i", "s/ & / \\&amp; /g", xmlFile) }
        } else {
            exec { commandLine("sed", "-i", "''", "s/ & / \\&amp; /g", xmlFile) }
        }
    }
}


tasks.register<Javadoc>("generateJdiffReport") {
    if (project.hasProperty("oldApi")) {
        val outputPath = project.findProperty("outputPath") as? String ?: "diffs-out"
        val jdiff = project.property("jdiffjar") as String
        val xerces = project.property("xercesjar") as String
        val oldApi = project.property("oldApi") as String
        val newApi = project.property("newApi") as String
        val newApiDir = project.property("newApiDir") as String
        val oldApiDir = project.property("oldApiDir") as String

        options.destinationDirectory = file(outputPath)
        // Collect all Java sources from the main source set
        val javaSources = android.sourceSets["main"].java.srcDirs
        source = fileTree(javaSources) { include("**/*.java") }

        classpath =
            files("${android.sdkDirectory}/platforms/${android.compileSdkVersion}/android.jar")
        options.doclet = "jdiff.JDiff"
        options.docletpath = listOf(file(jdiff), file(xerces))
        (options as? StandardJavadocDocletOptions)?.apply {
            addStringOption("subpackages", ".")
            addStringOption("newapidir", newApiDir)
            addStringOption("oldapidir", oldApiDir)
            addStringOption("oldapi", oldApi)
            addBooleanOption("verbose", true)
            addStringOption("newapi", newApi)
            // Remove -notimestamp if present (Doclava/JDiff do not support it)
            isNoTimestamp = false
        }
    }
}

val rClassPath =
    "build/generated/not_namespaced_r_class_sources/releaseUnitTest/processReleaseUnitTestResources/r/com/google/android/material/R.java"

afterEvaluate {
    val javadocTasks = listOfNotNull(
        tasks.findByName("generateJavadocs") as Javadoc,
        tasks.findByName("generateApiXml") as Javadoc
    )

    javadocTasks.forEach { task ->
        task.dependsOn(":lib:processReleaseUnitTestResources")
        task.source(task.source + files(rClassPath))

        val releaseVariant = android.libraryVariants.find { it.name == "release" }
        if (releaseVariant != null) {
            // Add transitive runtime dependencies to classpath
            val runtimeClasspath = releaseVariant.runtimeConfiguration.incoming
                .artifactView { attributes.attribute(ARTIFACT_TYPE, "android-classes") }.files
            task.classpath += runtimeClasspath

            // Add project and compile dependencies to classpath
            releaseVariant.javaCompileProvider.configure {
                task.classpath += releaseVariant.getCompileClasspath(null)
                task.classpath += task.project.files(destinationDirectory)
                task.source += this.source
            }
        }

        // Doclava does not understand -notimestamp option that is default since Gradle 6.0
        (task.options as? StandardJavadocDocletOptions)?.apply {
            isNoTimestamp = false
        }
    }
}

tasks.register<Jar>("androidSourcesJar") {
    archiveClassifier.set("sources")
    from(android.sourceSets["main"].java.srcDirs) {
        // Needed because we have Java sources and resources in same directory
        include("**/*.java")
        includeEmptyDirs = false
    }
}

val mdcLibraryVersion: String by rootProject.extra
val mdcLibraryPackage: String by rootProject.extra
val ghUsername: String by rootProject.extra
val ghAccessToken: String by rootProject.extra

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                afterEvaluate { from(components["release"]) }

                groupId = "sesl.$mdcLibraryPackage"
                artifactId = "material"
                version = mdcLibraryVersion

                pom {
                    name.set("SESL Material Components for Android")
                    description.set(
                        "SESL variant  of Material Components for Android library.\n" +
                                "Material Components for Android is a static library\n" +
                                "that you can add to your Android application in order to use\n" +
                                "APIs that provide implementations of the Material Design specification.\n" +
                                "Compatible on devices running API 14 or later."
                    )
                    url.set("https://github.com/tribalfs/sesl-material-components-android")
                    inceptionYear.set("2024")
                    developers {
                        developer {
                            id.set("tribalfs")
                            name.set("Tribalfs")
                            url.set("https://github.com/tribalfs")
                        }
                    }
                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                            distribution.set("repo")
                        }
                    }
                    scm {
                        connection = "scm:git:https://github.com/tribalfs/sesl-material-components-android.git"
                        url = "https://github.com/tribalfs/sesl-material-components-android"
                    }
                }
            }
        }

        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/tribalfs/sesl-material-components-android")
                credentials {
                    username = ghUsername
                    password = ghAccessToken
                }
            }
        }
    }
}

configurations.all {
    exclude(group = "androidx.core", module = "core")
    exclude(group = "androidx.core", module = "core-ktx")
    exclude(group = "androidx.customview", module = "customview")
    exclude(group = "androidx.coordinatorlayout", module = "coordinatorlayout")
    exclude(group = "androidx.appcompat", module = "appcompat")
    exclude(group = "androidx.fragment", module = "fragment")
    exclude(group = "androidx.recyclerview", module = "recyclerview")
    exclude(group = "androidx.viewpager", module = "viewpager")
    resolutionStrategy {
        componentSelection {
            all {
                if (candidate.version.matches(".*-sesl7.*".toRegex()) ||
                    candidate.version.matches(".*-sesl6.*".toRegex())) {
                    reject("Rejecting sesl6 and sesl7 versions")
                }
            }
        }
    }
}

dokka {
    dokkaPublications.html {
        moduleName.set("SESL Material")
        suppressObviousFunctions.set(true)
        failOnWarning.set(false)
        suppressInheritedMembers.set(true)
    }

    dokkaSourceSets.main {
        sourceRoots.from(file("java"))

        sourceLink {
            localDirectory.set(projectDir.resolve("java"))
            remoteUrl("https://github.com/tribalfs/sesl-material-components-android/blob/sesl/master/lib/java")
            remoteLineSuffix.set("#L")
        }

        externalDocumentationLinks {
            register("sesl.androidx") {
                url("https://tribalfs.github.io/sesl-androidx/")
                packageListUrl("https://tribalfs.github.io/sesl-androidx/package-list")
            }
        }

    }
}
