inputDir = process.argv[2];
outputDir = process.argv[3];

fs = require('fs');
path = require('path');

require("jalangi2").instrumentDir({
            inputFiles: [inputDir],
            no_html: true,
            outputDir: outputDir,
            inlineIID: true,
            inlineSource: true
        });

