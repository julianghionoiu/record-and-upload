

Command-line tool that records the screen as Fragmented MP4 and upload the fragments as they are being generated.


## Development

### Build and run as command-line app

This will grate a maven based Jar that will download the required dependencies before running the app:
```bash
./gradlew clean mavenCapsule
rm -R ~/.capsule/deps/ro
java -jar ./build/libs/record-and-upload-0.0.4-SNAPSHOT-capsule.jar --unique-id X \
            --config .private/aws-test-secrets --store ./build/play
```

To generate test files you could run
```bash
 mkdir -p build/play && dd if=/dev/random of=build/play/output_`date +%s`.mp4  bs=1m  count=16
```
