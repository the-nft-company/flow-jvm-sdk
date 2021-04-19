#!/bin/sh

./gradlew \
  -PsnapshotDate=$(date +'%Y%m%d%H%M%S') \
  -x test \
  clean \
  publishToSonatype \
  closeSonatypeStagingRepository
