import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.google.protobuf.gradle.*

plugins {
    kotlin("jvm") version "1.3.50"
    `java-library`
    id("com.google.protobuf") version "0.8.10"
    idea
    application
}

idea {
    module {
        sourceDirs.add(file("${projectDir}/src/generated/main/java"))
        sourceDirs.add(file("${projectDir}/src/generated/main/grpc"))
    }
}

val protobufVersion = "3.10.0"
val grpcVersion = "1.24.1"

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${protobufVersion}"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${grpcVersion}"
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
    api("com.google.protobuf:protobuf-java:${protobufVersion}")
    implementation("io.grpc:grpc-netty-shaded:1.24.0")
    implementation("io.grpc:grpc-protobuf:1.24.0")
    implementation("io.grpc:grpc-stub:1.24.0")
    api("javax.annotation:javax.annotation-api:1.3.2")

}
repositories {
    mavenCentral()
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

application {
    mainClassName = "MainKt"
}