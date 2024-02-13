pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {url = uri ("https://jitpack.io")}
    }
}

rootProject.name = "Melon"
include(":app")
include(":openCV")
//include(":ucrop", ":sample")