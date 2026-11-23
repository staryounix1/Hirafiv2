import java.io.File
import org.gradle.authentication.http.BasicAuthentication

pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven {
      url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
      authentication {
        create<BasicAuthentication>("basic")
      }
      credentials {
        username = "mapbox"
        password = run {
          val envFile = File(settingsDir, ".env")
          val fromEnvFile = if (envFile.exists()) {
            envFile.readLines()
              .asSequence()
              .map { it.trim() }
              .filter { it.startsWith("MAPBOX_DOWNLOADS_TOKEN=") }
              .map { it.substringAfter("=").trim().trim('"', '\'') }
              .firstOrNull { it.isNotBlank() && !it.startsWith("YOUR_") }
          } else null
          fromEnvFile
            ?: providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN").orNull?.takeIf {
              it.isNotBlank() && !it.startsWith("YOUR_")
            }
            ?: System.getenv("MAPBOX_DOWNLOADS_TOKEN")?.takeIf {
              it.isNotBlank() && !it.startsWith("YOUR_")
            }
            ?: ""
        }
      }
    }
  }
}

rootProject.name = "Hirafi"

include(":app")
