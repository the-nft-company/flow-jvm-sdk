#!/bin/sh

./gradlew \
  -x test \
  clean \
  publishToSonatype \
  closeSonatypeStagingRepository
