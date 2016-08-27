#!/bin/bash

#Stop on error
set -e

ROOT=$1
MAIN_FILE=$2
# The instrumentation will include all files in $dir if specified
DIR=$3

PROJECT="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
LIB="${PROJECT}/logger/lib"
DIST="${PROJECT}/logger/dist"
NODE=$(which node)
JARS=${LIB}/commons-io-2.5.jar:${LIB}/gson-2.3.1.jar:${LIB}/org.json-20120521.jar:${DIST}/jer.jar

MAIN_CLASS=dk.au.cs.casa.jer.Main

if [[ -z $DIR ]]; then
    java -cp $JARS $MAIN_CLASS $ROOT $MAIN_FILE $NODE $PROJECT
else
    java -cp $JARS $MAIN_CLASS $ROOT $MAIN_FILE $DIR $NODE $PROJECT 
fi
