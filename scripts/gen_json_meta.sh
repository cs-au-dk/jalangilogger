#!/usr/bin/env bash
test_file=$1

uname=`uname`
if [[ "$uname" == "Darwin" ]]; then
    shacmd="shasum"
else
    shacmd="sha1sum"
fi

#If test_file is a directory, then 
if [[ -d $test_file ]]; then
    strip_slash=${test_file%/}
    sha="$($shacmd ${strip_slash}/* | $shacmd)"
else
    sha="$($shacmd ${test_file})"
fi

sha="${sha:0:40}"
time="$(date +%s)"
json_rep="{\"sha\":\"${sha}\", \"time\":${time}}"
echo "$json_rep"

