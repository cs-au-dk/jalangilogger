Prerequisites
- Run `npm install` to install jalangi
- Define $JALANGI for the script files in the scripts directory

Creating log files from JS files
- Run scripts/createLogFiles (The logged files are placed in a folder named JalangiLogFiles)


Creating log files from HTML files

1. run `scripts/instrumentHTMLFiles.sh` (The instrumented files are placed in a folder named instrumentedHTMLFiles)
2. Run `npm start` (From inside the nodeJSServer folder) to start the nodeJS server
3. Open the instrumented HTML-file you wish to log in a browser	
4. Interact with the page and press p to save the log file  (The log file is saved in a folder named nodeJSServer/unchangedLogFiles)
5. If multiple files are to be logged go back to step 3 and use the other file.
6. Run TransformHtmlLogFiles.java to adjust the log file to use the same sourcelocations as TAJS does. The actual log files are now saved in a folder named JalangiLogFiles and are ready to be copied into the soundnessTester in TAJS
