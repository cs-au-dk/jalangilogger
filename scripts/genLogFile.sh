#!/bin/bash

#Stop on error
set -e

MAIN_FILE=$1

# The instrumentation will include all files in $dir if specified
DIR=$2

SCRIPT_FOLDER="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JALANGI_FOLDER=${SCRIPT_FOLDER%/*}

JARS=logger/lib/commons-io-2.5.jar:logger/lib/gson-2.3.1.jar:logger/lib/org.json-20120521.jar:logger/dist/jer.jar

MAIN_CLASS=dk.au.cs.casa.jer.Main

if [[ -z $DIR ]]; then
    java -cp $JARS $MAIN_CLASS $JALANGI_FOLDER $MAIN_FILE 
else
    java -cp $JARS $MAIN_CLASS $JALANGI_FOLDER $MAIN_FILE $DIR
fi
