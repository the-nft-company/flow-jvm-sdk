# Flow JVM SDK

![Maven Central](https://img.shields.io/maven-central/v/org.onflow/flow)

> :warning: This is an alpha release; functionality may change.

This is a minimal proof-of-concept SDK for JVM languages that provides
utilities to interact with the Flow blockchain.

At the moment, the SDK includes the following features:
- Communication with the [Flow Access API](https://docs.onflow.org/access-api) over gRPC 
- Transaction preparation and signing
- _Events parsing (coming soon)_

## Installation

To add this SDK to your project using Maven, use the following:

```xml
<dependency>
    <groupId>org.onflow</groupId>
    <artifactId>flow</artifactId>
    <version>0.20</version>
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
