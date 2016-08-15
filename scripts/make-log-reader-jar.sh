#!/bin/bash

set -e

BASE=`pwd`
SRC_LOG="${BASE}/src/java/src"
SRC_READ="${BASE}/log-readers/java/src"
LIB_LOG="${SRC_LOG}/../lib"
LIB_READ="${SRC_READ}/../lib"
DIST="${BASE}/dist"
BUILD="${BASE}/build"


mkdir -p ${BUILD}
rm -rf ${BUILD}
mkdir -p ${BUILD}
mkdir -p ${DIST}

cd ${SRC_LOG}
cp --parents `find . -name "*.java"` ${BUILD}
cd ${SRC_READ}
cp --parents `find . -name "*.java"` ${BUILD}

JARS="${LIB_READ}/gson-2.3.1.jar:${LIB_LOG}/commons-io-2.5.jar:${LIB_LOG}/org.json-20120521.jar"
echo $JARS
cd ${BUILD}
javac -cp ${JARS} `find . -name "*.java"`
cd ${BUILD}
jar cf ${DIST}/jer.jar `find . -name "*.class"`
