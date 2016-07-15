#!/bin/bash
set -e

ROOT="src/java";

LIB="${ROOT}/lib";
SRC="${ROOT}/src";

javac  -cp ${LIB}/org.json-20120521.jar ${SRC}/dk/au/cs/casa/jer/TransformHtmlLogFiles.java
java  -cp  ${SRC}:${LIB}/org.json-20120521.jar dk.au.cs.casa.jer.TransformHtmlLogFiles

unchangedLogfiles="nodeJSServer/UnchangedLogFiles"
LOG_FILES="$(find ${unchangedLogfiles} -name '*.log')"

for log_file in ${LOG_FILES[@]};
do
    non_prefixed_file_path="${log_file#*UnchangedLogFiles/}"
    test_file="test/${non_prefixed_file_path%.*}.html"
    new_log_file="JalangiLogFiles/${non_prefixed_file_path}"
    json_rep="$(./scripts/genJsonMeta.sh ${test_file})"
    ./scripts/prependLine.sh "${json_rep}" "${new_log_file}"
done


