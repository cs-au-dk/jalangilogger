#!/usr/bin/env bash
main_file=$1
dir=$2
tmp_folder="tmp/"

if [[ ! -d $tmp_folder ]]; then
    mkdir $tmp_folder
fi

main_file_name="$(basename $main_file)"
main_file_folder="$(dirname $main_file)"
main_file_folder_wo_test="${main_file_folder#*/}"

#If dir is set
if ! [[ -z $dir ]]; then
    node scripts/instrumentDirHelper.js "${dir}" "${tmp_folder}"
    json_rep="$(./scripts/gen_json_meta.sh "${main_file}" "${dir}")"
    instrumented_files_folder="${tmp_folder}${main_file_folder_wo_test}"
    instrumented_main_file="${instrumented_files_folder}/${main_file_name}"
    ./scripts/execute-standalone "${instrumented_main_file}"
else
    json_rep="$(./scripts/gen_json_meta.sh "${main_file}")"
    instrumented_main_file="${main_file_name/.js/_jalangi_.js}";
    ./scripts/instrument "${main_file}" "${tmp_folder}"
    ./scripts/execute-standalone "${tmp_folder}${instrumented_main_file}"
fi

outputFileLocation="JalangiLogFiles/${main_file_folder_wo_test}"
outputFileName="${main_file_name%.*}.log"
echo "${outputFileName}"
outputFilePath="${outputFileLocation}/${outputFileName}"

if [[ ! -d $outputFileLocation ]]; then
    mkdir -p "${outputFileLocation}"
fi
new_log_file_name="NEW_LOG_FILE.log"

sort "${new_log_file_name}" | uniq > "${outputFilePath}"
rm "${new_log_file_name}"
rm -r "${tmp_folder}" 

./scripts/prepend_line.sh "${json_rep}" "${outputFilePath}"
