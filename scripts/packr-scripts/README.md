# Scripts to build OS specific packages

The goal of this folder is to house scripts and artifacts that concern building OS specific packages containing the respective versions of record-and-upload jar files.

## Features

- Scripts for OSes like Linux, MacOS and Windows are available
- [Packr](https://github.com/libgdx/packr) has been used to build the artifacts - packr related docs on the packr repo
- [Packr](https://github.com/libgdx/packr) jar itself can be built by cloning the repo and running `mvn clean package` and can be used directly out of the box
- [Packr](https://github.com/libgdx/packr) allows any target to any target creation of packages, meaning if one can get the scripts to run in a given environment, then packages for all three OSes can be built in that environment 
- [Packr](https://github.com/libgdx/packr) provides OS specific launchers with each package but package created is transparent enough that it can be used without the launcher i.e. via the traditional `java -jar ...` method
- Linux and MacOS archives are compressed using the .tgz format preserving the access rights while Windows archives are compressed using zip to the .zip format
- Scripts build archives with the following naming convention record-and-upload-[jar release version]-[os-name].[compressed archive ext], for eg,
   - Linux: record-and-upload-v0.0.16-linux.tgz
   - MacOS: record-and-upload-v0.0.16-macos.tgz
   - Windows: record-and-upload-v0.0.16-windows.zip
- The scripts leave behind the expanded and compressed versions of the package, so one can inspect the contents before shipping it
- common-env-variables.sh contain OS specific flags and variables including Vendor / Maintainer details
- Icons for each of the OS specific packages can be provided and incorporated in the package building process

## Requirements / prerequisites

- The respective OS specific folders must contain the OS-specific JRE or JDK zipped (Java version 8 or higher, Packr will figure out how to extract the JRE from it and build the package)
- The respective OS specific folders must contain the OS-specific record-and-upload-[version].jar file 
- The scripts depend on these tools to run in any environment
  - bash or cygwin
  - zip
  - tar
  - [Packr](https://github.com/libgdx/packr) jar - must be present in the current folder [project folder]/scripts/packr-scripts

### How to create OS-specific JRE or JDK archives

This is again an OS independent task, download any JDK 8 or JRE 8 from a Java vendor, preferrably as a compressed archive file. Otherwise the binary installer will need to be installed on the OS dependent target.

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