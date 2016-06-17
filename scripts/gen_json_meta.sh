#!/usr/bin/env bash
test_file=$1

uname=`uname`
if [[ "$uname" == "Darwin" ]]; then
    shacmd="shasum"
else
    shacmd="sha1sum"
fi

sha="$($shacmd ${test_file})"
sha="${sha:0:40}"
time="$(date +%s)"
json_rep="{\"sha\":\"${sha}\", \"time\":${time}}"
echo "$json_rep"

