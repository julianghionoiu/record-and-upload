

Command-line tool that records the screen as Fragmented MP4 and upload the fragments as they are being generated.

## Run

### Download


Download the `record-and-upload-VERSION-capsule.jar` from `https://github.com/julianghionoiu/record-and-upload/releases/latest`

### Configure

Configuration for running this service should be placed in file `.private/aws-test-secrets` in Java Properties file format. For examples.

```properties
aws_access_key_id=ABCDEFGHIJKLM
aws_secret_access_key=ABCDEFGHIJKLM
s3_region=ap-southeast-1
s3_bucket=bucketname
s3_prefix=prefix/
```

### Run

#### pre-Java 9 versions

```bash
java -jar record-and-upload-VERSION-capsule.jar --config .private/aws-test-secrets --store ./build/play
```

#### Java 9 versions

```bash
java --illegal-access=warn  --add-modules=java.xml.bind,java.activation -jar record-and-upload-VERSION-capsule.jar --config .private/aws-test-secrets --store ./build/play
```


## Development - building

### Build as a thin maven-based Capsule

This will create a maven based Jar that will download the required dependencies before running the app:

```bash
./gradlew clean mavenCapsule --info
rm -R ~/.capsule/deps/ro
```

### Build as a OS specific fat Jar

This will crate a maven based Jar that will download the required dependencies before running the app:

```bash
./gradlew shadowJar -i -PvideoArch=macos
./gradlew shadowJar -i -PvideoArch=windows
./gradlew shadowJar -i -PvideoArch=linux
```


## Development - running

#### Running with pre-Java 9 versions

```bash
java -jar ./build/libs/record-and-upload-`cat version.txt`-capsule.jar --config .private/aws-test-secrets --store ./build/play
```

#### Running with Java 9

Ensure `JAVA_HOME` points to the Java 9 SDK home folder. Use `java -version` to check if the java launcher on the `PATH` is version 9.0 (aka 1.9) or higher.

```bash
java --illegal-access=warn  --add-modules=java.xml.bind,java.activation -jar ./build/libs/record-and-upload-`cat version.txt`-capsule.jar --config .private/aws-test-secrets --store ./build/play
```

## Development - Testing

### Unit tests

Run the test suite with Gradle:
```bash
./gradlew clean test --info --console=plain
```

### Packaging tests

Run the self-test on the generated jar file:
```bash
java -jar build/libs/record-and-upload-linux-*-all.jar --run-self-test
```


### To release

```bash
./gradlew release
git push --tags
git push
```
