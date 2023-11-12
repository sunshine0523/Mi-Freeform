//enable projects.x for implementation
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven(url = "https://maven.kr328.app/releases")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://maven.kr328.app/releases")
    }
}

rootProject.name = "Mi-Freeform"
include(
    ":app",
    ":hidden-api",
    ":freeform-server",
    ":service"
)
