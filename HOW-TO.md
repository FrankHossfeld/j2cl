# Buildung j2cl

## Prerequisites

### macOS
To build j2cl on a Mac, you need the following installations:

* XCode
* XCode Command Line Tools
* Homebrew
* Bazel
* Maven

#### Installing XCode
Install XCode using the App Store. Follow the installation instratuctions.

#### Installing Homebrew
Visit https://docs.brew.sh/Installation for installation instructions.

#### Installing Bazel
install **bazel** using **Homebrew**:

https://docs.bazel.build/versions/master/install-os-x.html

**this will avoid configuration problem on macOS Catlina caused by installations from unsigned developers!**

add **Bazel** to .bash_profile:

```
export BAZEL=/usr/local/Cellar/bazel/2.1.0/bin/bazel
```
add **Bazel** to the path:
```
export PATH=${BAZEL}:${PATH}
```

#### Installing Maven
To install Maven on macOS, download the latest version from here: https://maven.apache.org.

Unpack the archive and add Maven to your profile.



### Windows 10
To build j2cl on a Windows 10, you need the following installations:

* Chocolate
* Bazel
* Microsoft Visual C++ 14.0

#### Installing Chocolate
To install Cocolate follow the instructions listet here:
https://chocolatey.org/install

#### Installing Bazel
install **bazel** using **Chocolate**:

run
```
choco install bazel
```
inside the PowerShell.

Check, that Bazel is added to the **PATH** by entering the following command: `bazel --version`. In case this command returns no the current version of Bazel, add **Bazel** to the envirement variables:

```
export BAZEL=/usr/local/Cellar/bazel/2.1.0/bin/bazel
```
add **Bazel** to the path:
```
export PATH=${BAZEL}:${PATH}
```

#### Installing Microsoft Visual C++ 14.0
To install Microsoft Visiual Studio follow the instructions given here: https://docs.bazel.build/versions/master/windows.html#using



## Building j2cl

### Clone the j2cl repos
clone the j2cl repo from the vertispan fork:

https://github.com/Vertispan/j2cl

and clone the **closure-library**:

https://github.com/google/closure-library (will be needed for compilung j2cl)

## Build
Open the Windows PoweSehll and move to the root directory.
Then run:

1. `rm .bazelversion`

2. `bazel sync`

3. `cd maven`

4. `./build.sh` (on macOS) or `./build.cmd` (on Windows 10)








5. `mvn clean install`







### Working Maven versions:

* 3.6.1 -> not working
* 3.6.2 -> working
* 3.6.3 -> working

### Notes
It looks like it is impossibe to copy the workspace on another computer. The build will fail. (needs further investigions)




## Windows 10

### Clone the j2cl repos
clone the j2cl repo from the vertispan fork:

https://github.com/Vertispan/j2cl

and clone the **closure-library**:

https://github.com/google/closure-library (which will be needed for compilung j2cl)

### Building
inside the parent directory:

1. `rm .bazelversion`

2. `bazel sync`

3. `cd maven`

4. `./build.sh`

5. `mvn clean install`


### Working Maven versions:

* 3.6.1 -> not working
* 3.6.2 -> working
* 3.6.3 -> working

### Notes
It looks like it is impossibe to copy the workspace on another computer. The build will fail. (needs further investigions)


intalliere Visual C++
I just stumbled onto this issue accessing some Python libraries: Microsoft Visual C++ 14.0 is required. Get it with "Microsoft Visual C++ Build Tools". The latest link to that is actually here: https://visualstudio.microsoft.com/downloads/#build-tools-for-visual-studio-2019


The target you are compiling requires Visual C++ build tools.
Bazel couldn't find a valid Visual C++ build tools installation on your machine.
Please check your installation following https://docs.bazel.build/versions/master/windows.html#using