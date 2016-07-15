#!/bin/bash
mainFile=$1

# The instrumentation will include all files in $dir if specified
dir=$2

# Argument used when invoking the script from TAJS
shouldPutInResources=$3

tmpFolder="tmp"
instrumentOutFolder="test"
scriptLocationDir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
mainFileName="$(basename $mainFile)"
mainFileFolder="$(dirname $mainFile)"

#Create output folder for instrumented files
mkdir -p "$tmpFolder/$instrumentOutFolder"

#If dir is set
if ! [[ -z $dir ]]; then
    # NOTE. Jalangi will insert source locations relative to the output folder.
    # This is why entering the tmp folder before running the instrumentation is crucial
    # Otherwise the source locations will start with a 'tmp/'
    cd $tmpFolder
    node "$scriptLocationDir/instrumentDirHelper.js" "../${dir}" "$instrumentOutFolder"
    cd "../"
    jsonRep="$($scriptLocationDir/genJsonMeta.sh "${mainFile}" "${dir}")"
    instrumented_files_folder="${tmpFolder}/${mainFileFolder}"
    instrumented_mainFile="${instrumented_files_folder}/${mainFileName}"
    $scriptLocationDir/execute-standalone "${instrumented_mainFile}"
else
    jsonRep="$($scriptLocationDir/genJsonMeta.sh "${mainFile}")"
    instrumented_mainFile="${mainFileName/.js/_jalangi_.js}";
    $scriptLocationDir/instrument "${mainFile}" "${tmpFolder}"
    $scriptLocationDir/execute-standalone "${tmpFolder}/${instrumented_mainFile}"
fi

mainFileFolderWithoutTest="${mainFileFolder#*/}"

if [[ $shouldPutInResources ]]; then
    logFileFolder="resources/JalangiLogFiles/${mainFileFolderWithoutTest}"
else
    logFileFolder="JalangiLogFiles/${mainFileFolderWithoutTest}"
fi

outputFile="${mainFileName%.*}.log"
outputFilePath="${logFileFolder}/${outputFile}"

if [[ ! -d $logFileFolder ]]; then
    mkdir -p "${logFileFolder}"
fi

newLogFileName="NEW_LOG_FILE.log"

sort "${newLogFileName}" | uniq > "${outputFilePath}"
rm "${newLogFileName}"
rm -r "${tmpFolder}" 

$scriptLocationDir/prependLine.sh "${jsonRep}" "${outputFilePath}"
