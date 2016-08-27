#!/bin/bash

#Stop on error
set -e

MAIN_FILE=$1

# The instrumentation will include all files in $dir if specified
DIR=$2

SCRIPT_FOLDER="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT="${SCRIPT_FOLDER}/.."
LIB="${PROJECT}/logger/lib"
DIST="${PROJECT}/logger/dist"

JARS=${LIB}/commons-io-2.5.jar:${LIB}/gson-2.3.1.jar:${LIB}/org.json-20120521.jar:${DIST}/jer.jar

MAIN_CLASS=dk.au.cs.casa.jer.Main

if [[ -z $DIR ]]; then
    java -cp $JARS $MAIN_CLASS $PROJECT $MAIN_FILE 
else
    java -cp $JARS $MAIN_CLASS $PROJECT $MAIN_FILE $DIR
fi
