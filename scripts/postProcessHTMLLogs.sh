#!/usr/bin/env bash

set -e

ROOT="src/java";

LIB="${ROOT}/lib";
SRC="${ROOT}/src";

javac  -cp ${LIB}/org.json-20120521.jar ${SRC}/dk/au/cs/casa/jer/TransformHtmlLogFiles.java
java  -cp  ${SRC}:${LIB}/org.json-20120521.jar dk.au.cs.casa.jer.TransformHtmlLogFiles