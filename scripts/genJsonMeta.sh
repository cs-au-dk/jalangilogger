#!/bin/bash
test_file=$1
test_dir=$2

uname=`uname`
test_file_wo_path="$(basename test_file)"
shacmd="shasum"
PWD=$(pwd)

#If test_file is a directory, then 
if ! [[ -z $test_dir ]]; then
    strip_slash=${test_dir%/}
    sha=$((find $strip_slash -type f \( -exec $shacmd {} \; \); echo $test_file_wo_path) | $shacmd)
    test_file_root=$strip_slash
else
    sha="$($shacmd ${test_file})"
    test_file_root=$test_file
fi

sha="${sha:0:40}"
time="$(date +%s)"
json_rep="{\"sha\":\"${sha}\", \"time\":${time}, \"root\":\"${test_file_root}\"}"
echo "$json_rep"
