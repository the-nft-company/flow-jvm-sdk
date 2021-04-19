
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
$> ./.github/publish-snapshot.sh
```
If the `version` specified in the `build.gradle.kts` file is `1.2.3` then this script will release a 
SNAPSHOT version that looks something like this: `0.2.0.20210419134847-SNAPSHOT` where the `20210419134847`
portion is the year, month, day, hour, minutes, seconds that the build was cut.

## publishing a snapshot version

To release a non snapshot version run the following in the root directory of the repository:
```shell
$> ./.github/publish-snapshot.sh
```
Be sure that the `version` in the `build.gradle.kts` file is what you want it to be.
