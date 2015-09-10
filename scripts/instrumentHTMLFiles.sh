#!/usr/bin/env bash
for f in $(find test -name '*.html');
do
	echo "$f"
	saveDir="${f/test/}"    
    node node_modules/jalangi2/src/js/commands/instrument.js --inlineIID --inlineSource -i --inlineJalangi --analysis src/ValueLogger.js --outputDir instrumentedHtmlFiles/$saveDir "$f"
done