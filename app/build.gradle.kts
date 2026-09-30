plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.myapplication"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 36
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

abstract class PackageAppFunctionAssetsTask : DefaultTask() {
    @get:Inject
    abstract val fs: FileSystemOperations

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun packageAssets() {
        fs.copy {
            from(inputDir)
            into(outputDir)
            include("*.xml")
        }
    }
}

// AGP built-in Kotlin does not auto-wire KSP-generated resources. Package the
// AppFunctions XML assets via the Variant API so they land in the APK.
androidComponents {
    onVariants { variant ->
        val capitalized = variant.name.replaceFirstChar { it.uppercase() }
        val packageTask = tasks.register<PackageAppFunctionAssetsTask>(
            "package${capitalized}AppFunctionAssets",
        ) {
            dependsOn("ksp${capitalized}Kotlin")
            inputDir.set(
                layout.buildDirectory.dir("generated/ksp/${variant.name}/resources/assets"),
            )
            outputDir.set(
                layout.buildDirectory.dir("generated/appFunctionAssets/${variant.name}"),
            )
        }
        variant.sources.assets?.addGeneratedSourceDirectory(
            packageTask,
            PackageAppFunctionAssetsTask::outputDir,
        )
    }
}

ksp {
    arg("appfunctions:aggregateAppFunctions", "true")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.appfunctions)
    implementation(libs.okhttp)
    ksp(libs.androidx.appfunctions.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
