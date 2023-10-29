plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "io.sunshine0523.hidden_api"
}

dependencies {
    annotationProcessor(libs.hiddenapirefineannotationprocessor)
    compileOnly(libs.hiddenapirefineannotation)
}