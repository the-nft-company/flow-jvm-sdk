
// configuration variables
val javaTargetVersion   = "1.8"
val defaultGroupId      = "org.onflow"
val defaultVersion      = "0.2.0-SNAPSHOT"

// other variables
group = (project.findProperty("groupId")?.toString()?.ifBlank { defaultGroupId }) ?: defaultGroupId
version = when {
    project.hasProperty("version") -> {
        project.findProperty("version")!!
    }
    project.hasProperty("snapshotDate") -> {
        "${defaultVersion.replace("SNAPSHOT", "")}.${project.findProperty("snapshotDate")!!}-SNAPSHOT"
    }
    else -> {
        defaultVersion
    }
}

val isReleaseVersion    = !version.toString().endsWith("-SNAPSHOT")

plugins {
    id("org.jetbrains.dokka") version "1.4.20"
    kotlin("jvm") version "1.4.30"
    idea
    jacoco
    signing
    `maven-publish`
    id("io.github.gradle-nexus.publish-plugin") version "1.0.0"
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    jcenter()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.4.20")

    api("org.onflow:flow:0.21")

    api("com.github.TrustedDataFramework:java-rlp:1.1.20")

    api("org.bouncycastle:bcpkix-jdk15on:1.68")

    api(platform("com.fasterxml.jackson:jackson-bom:2.12.2"))
    api("com.fasterxml.jackson.core:jackson-core")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")

    testApi(platform("org.junit:junit-bom:5.7.1"))
    testApi("org.junit.jupiter:junit-jupiter")
    testApi("org.assertj:assertj-core:3.19.0")
}

tasks {

    test {
        useJUnitPlatform()
        testLogging {
            exceptionFormat 	= org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showExceptions 		= true
            showStackTraces 	= true
            showCauses 			= true
        }
        finalizedBy("jacocoTestReport")
    }

    compileKotlin {
        sourceCompatibility = javaTargetVersion
        targetCompatibility = javaTargetVersion

        kotlinOptions {
            jvmTarget = javaTargetVersion
        }
    }

    jacocoTestReport {
        dependsOn(test)
        reports {
            html.isEnabled = true
            xml.isEnabled = true
            csv.isEnabled = false
        }
    }

    jacoco {
        toolVersion = "0.8.5"
    }

    nexusPublishing {
        repositories {
            sonatype {
                username.set(project.findProperty("sonatypeUsername")?.toString())
                password.set(project.findProperty("sonatypePassword")?.toString())
            }
        }
    }

    val documentationJar by creating(Jar::class) {
        dependsOn(dokkaHtml)
        archiveClassifier.set("javadoc")
        from(dokkaHtml.get().outputs)
    }

    val sourcesJar by creating(Jar::class) {
        dependsOn(classes)
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    artifacts {
        add("archives", documentationJar)
        add("archives", sourcesJar)
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(project.components["java"])
                artifact(documentationJar)
                artifact(sourcesJar)

                pom {
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    name.set(project.name)
                    url.set("https://onflow.org")
                    description.set("The Flow Blockchain JVM SDK")
                    scm {
                        url.set("https://github.com/onflow/flow")
                        connection.set("scm:git:git@github.com/onflow/flow-jvm-sdk.git")
                        developerConnection.set("scm:git:git@github.com/onflow/flow-jvm-sdk.git")
                    }
                    developers {
                        developer {
                            name.set("Flow Developers")
                            url.set("https://onflow.org")
                        }
                    }
                }
            }
        }
    }

    signing {
        isRequired = isReleaseVersion && (withType<PublishToMavenRepository>().find {
            gradle.taskGraph.hasTask(it)
        } != null)

        useGpgCmd() // us
        sign(publishing.publications)
    }
}
