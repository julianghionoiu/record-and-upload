

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

```bash
java -jar record-and-upload-VERSION-capsule.jar --config .private/aws-test-secrets --store ./build/play
```


## Development

### Build and run as command-line app

This will grate a maven based Jar that will download the required dependencies before running the app:
```bash
./gradlew clean mavenCapsule
rm -R ~/.capsule/deps/ro
java -jar ./build/libs/record-and-upload-`cat version.txt`-capsule.jar --config .private/aws-test-secrets --store ./build/play
```

To generate test files you could run
```bash
 mkdir -p build/play && dd if=/dev/random of=build/play/output_`date +%s`.mp4  bs=1m  count=16
```

### To release

```bash
./gradlew release
git push --tags
git push
```
