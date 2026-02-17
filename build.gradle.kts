//import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.serialization") version "2.3.10"
//    id("com.vanniktech.maven.publish") version "0.28.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

kotlin {
    explicitApi()
}

//mavenPublishing {
//    coordinates(project.group.toString(), project.name, project.version.toString())
//
//    pom {
//        name.set("kpresence")
//        description.set("A lightweight, cross-platform Kotlin library for Discord Rich Presence interaction.")
//        url.set("https://github.com/vyfor/KPresence")
//        inceptionYear.set("2024")
//        licenses {
//            license {
//                name.set("MIT License")
//                url.set("https://opensource.org/licenses/MIT")
//            }
//        }
//        developers {
//            developer {
//                id.set("pandier")
//                name.set("Pandier")
//                url.set("https://pandier.dev")
//            }
//            developer {
//                id.set("vyfor")
//                name.set("vyfor")
//                url.set("https://github.com/vyfor/")
//            }
//        }
//        scm {
//            url.set("https://github.com/pandier/kpresence/")
//            connection.set("scm:git:git://github.com/pandier/kpresence.git")
//        }
//        issueManagement {
//            system.set("GitHub")
//            url.set("https://github.com/pandier/kpresemce/issues")
//        }
//    }
//
//    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
//
//    signAllPublications()
//}
