#!/usr/bin/env bash
line=$1
file=$2
echo "${line}" | cat - ${file} > /tmp/out && mv /tmp/out ${file}
