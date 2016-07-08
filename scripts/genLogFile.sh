#!/usr/bin/env bash
main_file=$1
dir=$2
should_put_in_resources=$3
tmp_folder="tmp"
instr_out_folder="$tmp_folder/test"
script_location_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [[ ! -d $instr_out_folder ]]; then
    echo "Creating $instr_out_folder"
    mkdir -p $instr_out_folder
fi

main_file_name="$(basename $main_file)"
main_file_folder="$(dirname $main_file)"

#If dir is set
if ! [[ -z $dir ]]; then
    cd $tmp_folder
    node "$script_location_dir/instrumentDirHelper.js" "../${dir}" "test"
    cd "../"
    json_rep="$($script_location_dir/genJsonMeta.sh "${main_file}" "${dir}")"
    instrumented_files_folder="${tmp_folder}/${main_file_folder}"
    instrumented_main_file="${instrumented_files_folder}/${main_file_name}"
    $script_location_dir/execute-standalone "${instrumented_main_file}"
else
    json_rep="$($script_location_dir/genJsonMeta.sh "${main_file}")"
    instrumented_main_file="${main_file_name/.js/_jalangi_.js}";
    $script_location_dir/scripts/instrument "${main_file}" "${tmp_folder}"
    $script_location_dir/scripts/execute-standalone "${tmp_folder}${instrumented_main_file}"
fi

main_file_folder_wo_test="${main_file_folder#*/}"

if [[ $should_put_in_resources ]]; then
    outputFileLocation="resources/JalangiLogFiles/${main_file_folder_wo_test}"
else
    outputFileLocation="JalangiLogFiles/${main_file_folder_wo_test}"
fi
outputFileName="${main_file_name%.*}.log"
outputFilePath="${outputFileLocation}/${outputFileName}"

if [[ ! -d $outputFileLocation ]]; then
    mkdir -p "${outputFileLocation}"
fi
new_log_file_name="NEW_LOG_FILE.log"

sort "${new_log_file_name}" | uniq > "${outputFilePath}"
rm "${new_log_file_name}"
rm -r "${tmp_folder}" 

$script_location_dir/prependLine.sh "${json_rep}" "${outputFilePath}"
