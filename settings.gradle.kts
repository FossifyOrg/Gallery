pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        jcenter()
        mavenCentral()
        maven(url = "https://artifactory.img.ly/artifactory/imgly")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        jcenter()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        maven(url = "https://artifactory.img.ly/artifactory/imgly")
    }
}

rootProject.name = "Gallery"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
include(":app")

// TODO: This will be deprecated in future. Migrate to the newer `pluginManagement { includeBuild() }` mechanism instead of explicitly substituting dependency.
/*includeBuild("../Commons") {
    dependencySubstitution {
        substitute(module("org.fossify:commons")).using(project(":commons"))
    }
}*/
