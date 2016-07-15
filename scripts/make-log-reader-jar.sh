#!/bin/bash

set -e

BASE=`pwd`
SRC="${BASE}/log-readers/java/src"
DIST="${BASE}/dist"
BUILD="${BASE}/build"

mkdir -p ${BUILD}
mkdir -p ${DIST}

cd ${SRC}
javac -cp ${SRC}/../lib/gson-2.3.1.jar -d ${BUILD} `find ${SRC} -name "*.java"`
cd ${BUILD}
jar cf ${DIST}/jer.jar `find . -name "*.class"`
