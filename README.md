# JalangiLogger

JalangiLogger(subject to change) records facts about concrete executions of JavaScript programs.

The collection of facts is done as a Jalangi-analysis, and the values are stored as JSON entries in a text file.
In this readme, the text file of JSON entries is known as a "log file".
 
Caveat: This is an unpolished prototype-tool, known limitations are listed in the bottom of this readme, and on the issue tracker of this project.
 
For examples of how to use the collected values, see the later parts of this readme. 

## Collected facts 

- collected facts are values of expressions, variables and properties at certain locations in programs
- facts are *not* qualified by contexts, they are purely syntactic
- collected values are rarely concrete values (due to efficiency and space concerns)
- collected values are abstracted wrt. the value lattice of [TAJS](https://github.com/cs-au-dk/TAJS)
  - user-allocated objects are abstracted by their allocation site
  - many natively allocated objects are abstracted by their canonical name
  - strings and numbers are abstracted immediately to their abstract counter parts
  - **these abstraction choices can easily be changed** -- contact esbena@cs.au.dk for support
    
The [JalangiLogFiles](JalangiLogFiles) directory contains some example log files.

## Using collected values 
 
- The [logger](logger) directory contains implementations for reading the log files.
- The [logger/src/dk/au/cs/casa/jer/entries](logger/src/dk/au/cs/casa/jer/entries) contains Java files that describes what an entry in a log file can look like (see JavaDoc for further information).
- The [logger/src/](logger/src/) log-reader implementation can be thought of as the log-reader reference implementation.

- The log files have corresponding JavaScript and HTML source files in the [test](test/) directory
  - for space reasons, log files have *not* been generated for all files in the test directory
  - some log file contains TAJS-specific functions, these are mocked in [logger/src/ValueLogger.js](logger/src/ValueLogger.js) in order to avoid unintended side-effects on the executed code

### Using log files in Java
 
Java version 1.8+ is required to run this implementation.
 
Example: 
```java
Set<IEntry> logEntries = new dk.au.cs.casa.jer.LogParser("myLog.log").getEntries();
  
```

- The script [scripts/make-log-reader-jar.sh](scripts/make-log-reader-jar.sh) produces a jar file at dist/jer.jar.
- jer.jar depends on gson, it is present at [logger/lib/gson-2.3.1.jar](logger/lib/gson-2.3.1.jar)


## Collecting more values

If the example log files that have been provided with this project are not sufficient, more can be created.

### Prerequisites

- run `npm install` to install Jalangi and other dependencies
- java (& javac) 1.6+ is required to run the produced log files for HTML files

### Creating log files from plain JavaScript files

The file [scripts/createLogFiles](scripts/createLogFiles) can be used to create log files for all JavaScript files in the [test](test/) directory (recursively).
It does so by essentially recording values appearing during `node test/x/y/z/file.js`.

The log files will be placed in the directory [JalangiLogFiles](JalangiLogFiles) in a subdirectory corresponding to the location of the JavaScript files.

Creating a log file for a single JavaScript application can be done
using [scripts/genLogFile.sh](scripts/genLogFile.sh) 
Example:
```
$ ./scripts/genLogFile.sh ./ test/anderson/anderson10.js 
Log file is located at: JalangiLogFiles/test/anderson/anderson10.log
$ wc -l JalangiLogFiles/test/anderson/anderson10.log
12 JalangiLogFiles/test/anderson/anderson10.log
```

It is also possible to create a log file for javascript programs that have dependencies. For example nodejs requires. This is done using `./scripts/genLogFile.sh ./ path/to/dir path/to/dir/mainfile.js` 

### Creating log files from HTML files

Creating a log file for a HTML based test is slightly more involved.
When invoking [scripts/genLogFile.sh](scripts/genLogFile.sh) with a HTML file as an arugment a browser instance running the HTML application is spawned. The user can then interact with the application to generate values. Once the user is done, the browser application is killed by pressing 'p', and the values are saved to a log file similar. For example:

```
$ ./scripts/genLogFile.sh ./ test/chromeexperiments/core.html
Press p when done interacting with the browser.
Log file is located at: /Users/torp/development/jalangilogger/JalangiLogFiles/test/chromeexperiments/core.log
$ wc -l JalangiLogFiles/test/chromeexperiments/core.log
     696 JalangiLogFiles/test/chromeexperiments/core.log
```

Note: After pressing 'p' the script might continue running for around 10 seconds.

## Use case: Testing unsoundness of a static analysis

The collected values can be used to find concrete examples of unsoundness in a static analysis.

The collection of values is done using a dynamic analysis that will observe a subset of all potential values.
In order to be sound, a static analysis will over-approximate the set of potential values in the program.
Specifically, the over-approximation needs to include all of the dynamically observed values.
 
A dynamically observed value is not over-approximated by the static analysis, then the analysis is unsound, with the dynamic value as a proof.

Example:

Consider the JavaScript program:

```javascript
var x = 0;
var y = 2;
var z = x + y;
```
The log file for the execution of this program reveals that `z` should be an `UINT` at line 3.
There are multiple ways an analysis could be observed to be unsound according to this fact: e.g.: the analysis does not believe line 3 is reachable
 or the analysis believes `z` is a string.

Note that the collected string and number values are abstracted immediately regardless of whether they could be represented by a single concrete value.
This means that a precise and sound analysis can actually under-approximate the collected string and number values without being unsound.

## Misc. limitations and oddities

- Semantic limitations and bugs of [Jalangi](https://github.com/Samsung/jalangi2) will influence the logs
  - the most serious limitation is the improper treatment of the with-statement
- The exact choice of source locations used in the log files can be discussed, for now it is recommended to work around surprising special cases in the tools that use the log files
  - (an alternative to source locations matching is an id assigned to every AST-node using a deterministic tree traversal) 
- log files for JavaScript files is done where only a single JavaScript file is instrumented, obtaining a log file for an entire application is not currently possible
- log files for JavaScript files will have nodejs-semantics and **not** browser-semantics, e.g. the value of `this` is not the global object.

- TODO cleanup in nodeJSServer: it uses way to many node-packages
- TODO actual JavaDoc in Java log parsing files
- TODO use proper temp directory for temporary/generated files 
- TODO use config file for all the different directory-dependencies in scripts

## Contributing

Pull requests bug reports on the issue tracker are welcome.
