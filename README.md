# JalangiLogger

JalangiLogger records facts about concrete executions of JavaScript programs.

The collection of facts is done as a Jalangi-analysis (see [README](javascript/README.md)), and the values are stored as JSON entries in a text file.
In this readme, the text file of JSON entries is known as a "log file".
 
## Collected facts 

- collected facts are values of expressions, variables and properties at certain locations in programs
- facts are *not* qualified by contexts, they are purely syntactic
- collected values are rarely concrete values (due to efficiency and space concerns)
- collected values are abstracted wrt. the value lattice of [TAJS](https://github.com/cs-au-dk/TAJS)
  - user-allocated objects are abstracted by their allocation site
  - many natively allocated objects are abstracted by their canonical name
 
    
## Creating log files

Log files can be created by instantiating [Logger](java/src/dk/au/cs/casa/jer/Logger.java) and invoking the run method. 

NB: since the javascript part of the project has external dependencies, the command `cd javascript; npm install` needs to be run before log files can be created.

## Using log files
 
- The [LogParser](java/src/dk/au/cs/casa/jer/LogParser.java) contains should be used for reading log files.
- The [logger/src/dk/au/cs/casa/jer/entries](java/src/dk/au/cs/casa/jer/entries) contains Java files that describes what an entry in a log file can look like (see JavaDoc for further information).

### Using log files
 
Java version 1.8+ is required to run this implementation.
 
Example: 
```java
Set<IEntry> logEntries = new dk.au.cs.casa.jer.LogParser("myLog.log").getEntries();
  
```

## Building

The Java classes mentioned above can be bundled in a jar using gradle:
```
$ ./gradlew jar
$ ls -l build/libs/jalangilogger-*.jar
```

## Misc. limitations and oddities

- Semantic limitations and bugs of [Jalangi](https://github.com/Samsung/jalangi2) will influence the logs
  - the most serious limitation is the improper treatment of the with-statement
- The exact choice of source locations used in the log files can be discussed, for now it is recommended to work around surprising special cases in the tools that use the log files
  - (an alternative to source locations matching is an id assigned to every AST-node using a deterministic tree traversal) 
- log files for JavaScript files is done where only a single JavaScript file is instrumented, obtaining a log file for an entire application is not currently possible
- log files for JavaScript files will have nodejs-semantics and **not** browser-semantics, e.g. the value of `this` is not the global object.

- TODO actual JavaDoc in Java log parsing files

## Contributing

Pull requests bug reports on the issue tracker are welcome.
