# Flow JVM SDK

[![Maven Central](https://img.shields.io/maven-central/v/org.onflow/flow-jvm-sdk)](https://search.maven.org/search?q=g:org.onflow%20AND%20a:flow-jvm-sdk)

> :warning: This is an alpha release; functionality may change.

This is a minimal proof-of-concept SDK for JVM languages that provides
utilities to interact with the Flow blockchain.

At the moment, the SDK includes the following features:
- [x] Communication with the [Flow Access API](https://docs.onflow.org/access-api) over gRPC 
- [x] Transaction preparation and signing
- [*] Events parsing 

## Installation

To add this SDK to your project using Maven, use the following:

```xml
<dependency>
    <groupId>org.onflow</groupId>
    <artifactId>flow-jvm-sdk</artifactId>
    <version>0.1</version>
</dependency>
```

## Example Usage

Check out the [example repository](https://github.com/onflow/flow-java-client-example) for an example
of how to use this SDK in a Java application.

## Contribution

Project is in the very early phase, all contributions are welcomed.

Read the [contributing guide](https://github.com/onflow/flow-jvm-sdk/blob/main/CONTRIBUTING.md) to get started.

## Dependencies

Java Developer Kit (JDK) 8 or better
