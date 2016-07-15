#!/bin/bash
test_file=$1
test_dir=$2

uname=`uname`
test_file_wo_path="$(basename test_file)"
if [[ "$uname" == "Darwin" ]]; then
    shacmd="shasum"
else
    shacmd="sha1sum"
fi

#If test_file is a directory, then 
if ! [[ -z $test_dir ]]; then
    strip_slash=${test_dir%/}
    sha="$(($shacmd ${strip_slash}/* | cut -c-40; echo $test_file_wo_path) | $shacmd)"
else
    sha="$($shacmd ${test_file})"
fi

sha="${sha:0:40}"
time="$(date +%s)"
json_rep="{\"sha\":\"${sha}\", \"time\":${time}}"
echo "$json_rep"
