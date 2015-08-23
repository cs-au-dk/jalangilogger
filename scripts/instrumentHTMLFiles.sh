#!/usr/bin/env bash
if [ -z ${JALANGI+x} ]; then
    >&2 echo '$JALANGI is not defined?! Set to e.g. "node_modules/jalangi2"';
    exit 1;
fi
for f in $(find test -name '*.html');
do
	echo "$f"
	saveDir="${f/test/}"    
    node ${JALANGI}/src/js/commands/instrument.js --inlineIID --inlineSource -i --inlineJalangi --analysis src/ValueLoggerBrowser.js --outputDir instrumentedHtmlFiles/$saveDir "$f"
done