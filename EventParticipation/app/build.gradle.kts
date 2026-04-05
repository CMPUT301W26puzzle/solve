plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.eventparticipation"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.eventparticipation"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation(platform("com.google.firebase:firebase-bom:34.10.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.1")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Test dependencies
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.11.1")

    // Android Test dependencies using TOML aliases
    androidTestImplementation(libs.core)
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.appcompat:appcompat:1.6.1")
    androidTestImplementation(libs.test.rules)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.espresso.intents)
    androidTestImplementation(libs.espresso.contrib) {
        exclude(group = "com.google.protobuf", module = "protobuf-lite")
    }
}

tasks.register<Javadoc>("generateJavadoc") {
    source = fileTree("src/main/java")
    title = "Event Participation App Javadocs"
    isFailOnError = false
    destinationDir = project.rootProject.file("javadocs")
    val androidExt = project.extensions.getByType(com.android.build.gradle.AppExtension::class.java)
    classpath += project.files(androidExt.bootClasspath)
    androidExt.applicationVariants.all {
        if (name == "debug") { // Use the debug variant's classpath
            classpath += javaCompileProvider.get().classpath
        }
    }
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:none", "-quiet")
        locale = "en_US"
    }
}