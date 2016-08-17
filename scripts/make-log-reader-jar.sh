#!/bin/bash
set -e

BASE=`pwd`
SRC="${BASE}/logger/src"
LIB="${BASE}/logger/lib"
DIST="${BASE}/logger/dist"
BUILD="${BASE}/logger/build"
OS=$(uname)


mkdir -p ${BUILD}
rm -rf ${BUILD}
mkdir -p ${BUILD}
mkdir -p ${DIST}

cd ${SRC}

if [[ $OS == "Darwin" ]]; then
    rsync -R `find . -name "*.java"` ${BUILD}
else
    cp --parents `find . -name "*.java"` ${BUILD}
fi

JARS="${LIB}/gson-2.3.1.jar:${LIB}/commons-io-2.5.jar:${LIB}/org.json-20120521.jar"
echo $JARS
cd ${BUILD}
javac -cp ${JARS} `find . -name "*.java"`
cd ${BUILD}
jar cf ${DIST}/jer.jar `find . -name "*.class"`
