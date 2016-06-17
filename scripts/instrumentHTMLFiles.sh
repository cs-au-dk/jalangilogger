#!/usr/bin/env bash

if [ -z "$1" ]
  then
    for f in $(find test -name '*.html' -o -name '*.htm');
    do
        echo "$f"
        saveDir="${f/test/}"
        node node_modules/jalangi2/src/js/commands/instrument.js --inlineIID --inlineSource -i --inlineJalangi --analysis src/ValueLogger.js --outputDir instrumentedHtmlFiles/$saveDir "$f"
    done
  else
    node node_modules/jalangi2/src/js/commands/instrument.js --inlineIID --inlineSource -i --inlineJalangi --analysis src/ValueLogger.js --outputDir instrumentedHtmlFiles/$1 "test/$1"
fi
