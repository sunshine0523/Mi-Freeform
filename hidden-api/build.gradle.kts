plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "io.sunshine0523.hidden_api"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    annotationProcessor(libs.hiddenapirefineannotationprocessor)
    compileOnly(libs.hiddenapirefineannotation)
}