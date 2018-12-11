# Scripts to build OS specific packages

The goal of this folder is to house scripts and artifacts that concern building OS specific packages containing the respective versions of record-and-upload jar files.

## Features

- Scripts for OSes like Linux, MacOS and Windows are available
- Linux and MacOS archives are compressed using the .tgz format preserving the access rights while Windows archives are compressed using zip to the .zip format
- Scripts build archives with the following naming convention record-and-upload-[jar release version]-[os-name].[compressed archive ext], for eg,
   - Linux: record-and-upload-v0.0.16-linux.tgz
   - MacOS: record-and-upload-v0.0.16-macos.tgz
   - Windows: record-and-upload-v0.0.16-windows.zip
- The scripts leave behind the expanded and compressed versions of the package, so one can inspect the contents before shipping it
- common-env-variables.sh contain OS specific flags and variables including Vendor / Maintainer details
- Icons for each of the OS specific packages can be provided and incorporated in the package building process
- Packr.jar is called by the scripts and it takes care of the following:
  - creates a folder structure that is suitable and required for the OS of choice
  - unpacks the JRE archive and lays it out in the folder structure
  - creates a OS specific launcher
  - creates a config.json which is passed in as JVM args when the launcher is run
  - places all the above in the necessary fashion inside the folder structure read to be used when unpacked

## Requirements / prerequisites

- The respective OS specific folders must contain the OS-specific JRE or JDK zipped (Java version 8 or higher, Packr will figure out how to extract the JRE from it and build the package)
- The respective OS specific folders must contain the OS-specific record-and-upload-[version].jar file 
- The scripts depend on these tools to run in any environment
  - bash or cygwin
  - zip
  - tar
  - [Packr](https://github.com/libgdx/packr) jar - must be present in the current folder [project folder]/scripts/packr-scripts

### About Packr

- [Packr](https://github.com/libgdx/packr) has been used to build the artifacts - packr related docs on the packr repo
- [Packr](https://github.com/libgdx/packr) jar itself can be built by cloning the repo and running `mvn clean package` and can be used directly out of the box
- [Packr](https://github.com/libgdx/packr) allows any target to any target creation of packages, meaning if one can get the scripts to run in a given environment, then packages for all three OSes can be built in that environment 
- [Packr](https://github.com/libgdx/packr) provides OS specific launchers with each package but package created is transparent enough that it can be used without the launcher i.e. via the traditional `java -jar ...` method

OS-specific packages are built using the respective scripts (which pass the required parameters to the `packr.jar` file). `packr.jar` creates the OS-specific packages for us.

### How to create OS-specific JRE or JDK archives

This is again an OS independent task, download any JDK 8 or JRE 8 from a Java vendor, preferrably as a compressed archive file. Otherwise the binary installer will need to be installed on the OS dependent target.

Please download the initial JRE/JDK binaries from one of the following sources in the order of presedence due to the level of testing and trust on the quality of the JDK/JRE binaries produced by them

- Oracle
- Azul Systems
- AdoptOpenJDK (https://adoptopenjdk.net/) or Amazon Corretto

#### Compressed archive
Place this compressed archive in the scripts/packr-scripts/[OS] folder of the respective operating system.

#### Binary installer
Run the installer on the target OS where it is meant to be run, locate the target folder where it has been installed. Usually it these types of folders we are looking for:

**Linux**
/usr/lib/jvm/[jdk/jre folder]

**MacOS**
/Library/Java/JavaVirtualMachines/[jdk/jre folder]

**Windows**
C:\Program Files\Java\[jdk/jre folder]

Do in the target folder for the respective OS, and zip the [jdk/jre folder] folder as a .zip file. Packr does not understand any other format irrespective of the target OS. Ensure the name of the archive is of the following format jre[...].zip. The script looking for it will look for such files, any other name means it won't be able to find the JDK and the process will terminate.

Once done, place this compressed archive in the `scripts/packr-scripts/[OS]` folder of the respective operating system.

### How to run the OS-specific packages

**Linux**
Unpack the .tgz archive in the target folder
A folder by the name record is unpacked
Record contains the below (skeleton structure):

```
record
    ├── record-and-upload
    ├── config.json
    ├── jre
    ├── libhumblevideo.so
    └── record-and-upload-v0.0.16.jar
```

Run command:

```
$ ./record/record-and-upload [necessary parameters]
```

The `template-for-recording/record_and_upload.sh` script in the `tdl-lord-of-runners` also details how the Linux packager is run.

**MacOS**
Unpack the .tgz archive in the target folder
A folder by the name record is unpacked
Record contains the below (skeleton structure):

```
record
└── Contents
    ├── Info.plist
    ├── MacOS
    │   └── record-and-upload
    └── Resources
        ├── config.json
        ├── jre
        ├── libhumblevideo.dylib
        └── record-and-upload-v0.0.16.jar
```

Run command:
In theory the below should work on the MacOS:

```
$ ./record/Contents/MacOS/record-and-upload [necessary parameters]
```

But due to an issue* with the launcher (record-and-upload), the below works:

```
$ ./record/Contents/Resources/jre/bin/java -jar [/path/to/record-and-upload-v0.0.16.jar] [necessary parameters]
```

The `template-for-recording/record_and_upload.sh` script in the `tdl-lord-of-runners` also details how the MacOS packager is run.


* the issue has been reported and discussed at https://github.com/libgdx/packr/issues/94 and https://github.com/libgdx/packr/issues/94#issuecomment-444537018. It could be that the dependencies need to be next to the launcher for it to work. Needs to be investigated, if the above workaround is not good enough.

**Windows**
Unzip the .zip archive in the target folder
A folder by the name record is unpacked
Record contains the below (skeleton structure):

```
record
    ├── record-and-upload.exe
    ├── config.json
    ├── jre
    ├── libhumblevideo-0.dll
    └── record-and-upload-v0.0.16.jar
```

Run command:

```
$ ./record/record-and-upload.exe [necessary parameters]
```

The `template-for-recording/record_and_upload.bat` script in the `tdl-lord-of-runners` also details how the Windows packager is run.
