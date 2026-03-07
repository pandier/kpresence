plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("org.jetbrains.dokka") version "2.1.0"
    id("com.vanniktech.maven.publish") version "0.36.0"
    id("org.jetbrains.changelog") version "2.2.1"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

kotlin {
    jvmToolchain(17)
    explicitApi()
}

mavenPublishing {
    pom {
        name.set("kpresence")
        description.set("A Kotlin library for easy Discord Rich Presence integration.")
        url.set("https://github.com/pandier/kpresence")
        inceptionYear.set("2024")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("pandier")
                name.set("Pandier")
                url.set("https://pandier.dev")
            }
            developer {
                id.set("vyfor")
                name.set("vyfor")
                url.set("https://github.com/vyfor")
            }
        }
        scm {
            url.set("https://github.com/pandier/kpresence")
            connection.set("scm:git:git://github.com/pandier/kpresence.git")
        }
        issueManagement {
            system.set("GitHub")
            url.set("https://github.com/pandier/kpresence/issues")
        }
    }

    publishToMavenCentral()
    signAllPublications()
}

changelog {
    groups.empty()
    repositoryUrl.set("https://github.com/pandier/kpresence")
}
