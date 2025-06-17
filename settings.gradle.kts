pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)  // Альтернативный вариант
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }  // Если нужны дополнительные репозитории
    }
}
rootProject.name = "Eproject"
include(":app")