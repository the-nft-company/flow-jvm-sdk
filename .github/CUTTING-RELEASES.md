
# How to cut a release

## configuration

First, create a `gradle.properties` file in the root of the project with the following properties:
```properties
signing.gnupg.executable=gpg
signing.gnupg.useLegacyGpg=false
#signing.gnupg.homeDir=gnupg-home
#signing.gnupg.optionsFile=gnupg-home/gpg.conf
signing.gnupg.keyName=XXXX
signing.gnupg.passphrase=XXXX

# only use this if you're overriding the group id
#groupId=com.nftco

sonatype.nexusUrl=https://s01.oss.sonatype.org/service/local/
sonatype.snapshotRepositoryUrl=https://s01.oss.sonatype.org/content/repositories/snapshots/
sonatype.username=XXXX
sonatype.password=XXXX
```

The URLs may differ depending on your setup. The URLs above are for the OSSRH repositories hosted
by sonatype. More information on the signing process can be found [here](https://docs.gradle.org/current/userguide/signing_plugin.html),
and more information on the release process can be found here [here](https://github.com/gradle-nexus/publish-plugin).

You will need to have gpg setup on your machine.

## publishing a snapshot version

To release a snapshot version run the following in the root directory of the repository:
```shell
$> ./gradlew \
  -PsnapshotDate=$(date +'%Y%m%d%H%M%S') \
  -x test \
  clean \
  publishToSonatype \
  closeAndReleaseSonatypeStagingRepository
```
If the `version` specified in the `build.gradle.kts` file is `1.2.3` then this script will release a 
SNAPSHOT version that looks something like this: `0.2.0.20210419134847-SNAPSHOT` where the `20210419134847`
portion is the year, month, day, hour, minutes, seconds that the build was cut.

## publishing a snapshot version

To release a non snapshot version run the following in the root directory of the repository:
```shell
$> ./gradlew \
  -x test \
  clean \
  publishToSonatype \
  closeAndReleaseSonatypeStagingRepository
```
Be sure that the `version` in the `build.gradle.kts` file is what you want it to be.

## continous Integration / Deployment

In the case of a CI/CD machine you may not want ot have the keyring file(s) on your machine, in this
case you can instead use an ascii armored version of the pgp key by passing the following arguments:

```shell
$> ./gradlew \
  -Psigning.key=${ASCII_ARMORED_VERSION_OF_PGP_KEY} \
  -Psigning.password=${PGP_KEY_PASSWORD} \
  -Psonatype.nexusUrl=... \
  -Psonatype.snapshotRepositoryUrl=... \
  -Psonatype.username=... \
  -Psonatype.password=... \
  -x test \
  clean \
  publishToSonatype \
  closeAndReleaseSonatypeStagingRepository
```

Be sure to pass the `-PsnapshotDate=$(date +'%Y%m%d%H%M%S')` if it's a snapshot

## github actions

There are two github actions configured:
- SNAPSHOT: On every commit to the `main` branch a build is performed and if successful it is deployed as a snapshot version.
- RELEASE: Whenever a tag is creatd with the pattern of `vXXX` a version with the name XXX is built and if successful deployed as a release version.

The following github secrets configure these actions:

- `FLOW_JVM_SDK_CICD_ENABLED`: Must be `true` for the actions to run
- `FLOW_JVM_SDK_CICD_PUBLISH_ENABLED`: Must be `true` for the publishing of artifacts to happen
- `FLOW_JVM_SDK_GROUP_ID`: optional groupId, defaults to `org.onflow`
- `FLOW_JVM_SDK_SIGNING_KEY`: ascii armored version of the pgp key for signing releases
- `FLOW_JVM_SDK_SIGNING_PASSWORD`: password to the pgp key
- `FLOW_JVM_SDK_NEXUS_URL`: nexus url for performing releases
- `FLOW_JVM_SDK_SNAPSHOT_REPOSITORY_URL`: nexus url for for permforming releases
- `FLOW_JVM_SDK_SONATYPE_USERNAME`: sonatype username
- `FLOW_JVM_SDK_SONATYPE_PASSWORD`: sonatype password

