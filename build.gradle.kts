import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.30"
    idea
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "1.0.0"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    api("com.github.TrustedDataFramework:java-rlp:1.1.20")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    api("org.onflow:flow:0.21")

    testImplementation(platform("org.junit:junit-bom:5.7.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.19.0")
    api("org.bouncycastle:bcpkix-jdk15on:1.68")
}

tasks.test {
    useJUnitPlatform()
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

nexusPublishing {
    repositories {
        sonatype()
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

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
    useGpgCmd() //use gpg2
    sign(publishing.publications["mavenJava"])
}

java {
    withJavadocJar()
    withSourcesJar()
}


group = "org.onflow"

// TODO - grab version from Git
version = "0.1.1"

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
