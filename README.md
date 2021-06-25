# Flow JVM SDK

[![Maven Central](https://img.shields.io/maven-central/v/org.onflow/flow-jvm-sdk)](https://search.maven.org/search?q=g:org.onflow%20AND%20a:flow-jvm-sdk)

> :warning: This is an alpha release not yet intended for production use. Functionality may change, please use at your own risk.

The Flow JVM SDK is a library for JVM languages (e.g. Java, Kotlin) that provides
utilities to interact with the Flow blockchain.

At the moment, this SDK includes the following features:
- [x] Communication with the [Flow Access API](https://docs.onflow.org/access-api) over gRPC 
- [x] Transaction preparation and signing
- [x] Cryptographic key generation, parsing, and signing
- [x] Marshalling & unmarshalling of [JSON-Cadence](https://docs.onflow.org/cadence/json-cadence-spec/)
- [x] DSL for creating, signing, and sending transactions and scripts

## Installation

Use the configuration below to add this 
SDK to your project using Maven or Gradle.

### Maven

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependency>
  <groupId>org.onflow</groupId>
  <artifactId>flow-jvm-sdk</artifactId>
  <version>[VERSION HERE]</version>
</dependency>
```

### Gradle

```groovy
repositories {
    ...
    maven { url 'https://jitpack.io' }
}

dependencies {
    api("org.onflow:flow-jvm-sdk:[VERSION HERE]")
}
```

### Gradle (with test extensions)

```groovy
plugins {
    ...
    id("java-test-fixtures")
}

repositories {
    ...
    maven { url 'https://jitpack.io' }
}

dependencies {
    api("org.onflow:flow-jvm-sdk:[VERSION HERE]")
    testFixturesApi(testFixtures("com.nftco:flow-jvm-sdk:[VERSION HERE]"))
}
```

The jitpack.io repository is necessary to access some of the dependencies of this library that are not available on Maven Central.

## Example usage

Check out the [example repository](https://github.com/onflow/flow-java-client-example) for an example
of how to use this SDK in a Java application.

## Integration tests

Tests annotated with `FlowEmulatorTest` depend on the [Flow Emulator](https://github.com/onflow/flow-emulator), which is part of the [Flow CLI](https://github.com/onflow/flow-cli) to be installed on your machine.

## Contribute to this SDK

This project is in the very early phase; all contributions are welcomed.

Read the [contributing guide](https://github.com/onflow/flow-jvm-sdk/blob/main/CONTRIBUTING.md) to get started.

## Dependencies

This SDK requires Java Developer Kit (JDK) 8 or newer.

## Credit

The Flow JVM SDK is maintained by 
[@briandilley](https://github.com/briandilley) and 
[@jereanon](https://github.com/jereanon) from
[The NFT Company](https://nftco.com/).

[![NFTco](nftco.svg)](https://nftco.com/)
