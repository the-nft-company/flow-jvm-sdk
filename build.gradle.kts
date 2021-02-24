import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.google.protobuf.gradle.*

plugins {
    kotlin("jvm") version "1.4.30"
    `java-library`
    id("com.google.protobuf") version "0.8.15"
    idea
    application
}

idea {
    module {
        generatedSourceDirs.addAll(listOf(
            file("${protobuf.protobuf.generatedFilesBaseDir}/main/grpc"),
            file("${protobuf.protobuf.generatedFilesBaseDir}/main/java")
        ))
    }
}

val protobufVersion = "3.14.0"
val grpcVersion = "1.35.0"

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
            }
        }
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api("com.google.protobuf:protobuf-java:$protobufVersion")
    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    api("javax.annotation:javax.annotation-api:1.3.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.+")
    implementation("com.github.TrustedDataFramework:java-rlp:1.1.20")

    protobuf("com.github.onflow:flow:m4ksio~pack-protobuf-into-jar-SNAPSHOT")

    testImplementation(platform("org.junit:junit-bom:5.7.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.19.0")
    api("org.bouncycastle:bcpkix-jdk15on:1.68")
}
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

tasks.test {
    useJUnitPlatform()
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}