## macOS
install **bazel** using **Homebrew**:

https://docs.bazel.build/versions/master/install-os-x.html

**this will avoid configuration problem on macOS Catlina caused by installations from unsigned developers!**

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
