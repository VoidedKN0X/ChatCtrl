pluginManagement {
    // No gradlePluginPortal() on purpose. Every plugin this build uses is on Google's Maven or Maven
    // Central. With the portal listed, a short Maven Central outage made Gradle take a Kotlin plugin
    // marker from the portal instead; the portal serves a different file for it, so dependency
    // verification failed with "... from repository Gradle Central Plugin Repository". If a plugin
    // that only exists on the portal is ever needed, add the portal back limited to that plugin:
    // gradlePluginPortal { content { includeGroup("<plugin id>"); includeGroup("<its library's group>") } }
    repositories {
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // TLS certificate checks for the Rust SDK go through Android's own trust store via this small library.
        exclusiveContent {
            forRepository {
                maven { url = uri("https://github.com/rustls/rustls-platform-verifier/raw/maven-archive/android-release-support/maven/") }
            }
            filter { includeModule("org.rustls", "rustls-platform-verifier") }
        }
        google()
        mavenCentral()
        // Recent builds of the Matrix Rust SDK bindings are also published as GitHub release assets.
        ivy {
            url = uri("https://github.com/matrix-org/matrix-rust-components-kotlin/releases/download")
            patternLayout { artifact("sdk-[revision]/[artifact]-[revision](-[classifier])(.[ext])") }
            metadataSources { gradleMetadata() }
            content { includeModule("org.matrix.rustcomponents", "sdk-android") }
        }
    }
}
rootProject.name = "ChatCtrl"
include(":app")
