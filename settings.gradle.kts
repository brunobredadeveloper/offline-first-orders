pluginManagement { repositories { mavenCentral(); google(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { mavenCentral(); google() } }
rootProject.name = "OfflineFirstOrders"
include(":core")
if (providers.gradleProperty("includeAndroid").orNull == "true") include(":app")
