

Command-line tool that records the screen as Fragmented MP4 and upload the fragments as they are being generated.


## Development

### Build and run as command-line app

This will grate a maven based Jar that will download the required dependencies before running the app:
```
./gradlew mavenCapsule
java -jar ./build/libs/record-and-upload-0.0.4-SNAPSHOT-capsule.jar --destination ./build/play --config .private/aws-test-secrets
```
