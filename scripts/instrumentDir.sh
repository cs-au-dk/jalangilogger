#!/usr/bin/env bash
dir=$1
main_file=$2
tmp_folder="tmp/"

if [[ ! -d $tmp_folder ]]; then
    mkdir $tmp_folder
fi

node scripts/instrumentDirHelper.js "${dir}" "${tmp_folder}"

json_rep="$(./scripts/gen_json_meta.sh "${dir}")"

main_file_name="$(basename $main_file)"
main_file_folder="$(dirname $main_file)"
main_file_folder_wo_test="${main_file_folder#*/}"
instrumented_files_folder="${tmp_folder}${main_file_folder_wo_test}"
instrumented_main_file="${instrumented_files_folder}/${main_file_name}"

node --harmony --max_old_space_size=4096 node_modules/jalangi2/src/js/commands/direct.js --analysis src/ValueLogger.js "${instrumented_main_file}" 

outputFileLocation="JalangiLogFiles/${main_file_folder_wo_test}"
outputFileName="${main_file_name%.*}.log"
echo "${outputFileName}"
outputFilePath="${outputFileLocation}/${outputFileName}"

if [[ ! -d $outputFileLocation ]]; then
    mkdir -p "${outputFileLocation}"
fi

sort "NEW_LOG_FILE.log" | uniq > "${outputFilePath}"

./scripts/prepend_line.sh "${json_rep}" "${outputFilePath}"
