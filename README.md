...
- Run `npm install` to install jalangi
- Define $JALANGI for the script files in the scripts directory
...

...
Creating log files from JS files
- Run scripts/createLogFiles (The logged files are placed in a folder named JalangiLogFiles)
...

...
Instrumentation in browser
How to log files:
	1. run `scripts/instrumentedHTMLFiles.sh` (The instrumented files are placed in a folder named instrumentedHTMLFiles)
	2. Run `npm start` (From inside the nodeJSServer folder) to start the nodeJS server
	3. Open the instrumented HTML-file you wish to log in a browser
		3.1 - Interact with the page and press p to save the log file  (The log file is saved in a folder named nodeJSServer/unchangedLogFiles)
	4. If multiple files are to be logged repeat step 3 with a new file.
	5. Run TransformHtmlLogFiles.java to adjust the log file to use the same sourcelocations as TAJS does.
		The actual log files are now saved in a folder named JalangiLogFiles and are ready to be copied into the soundnessTester in TAJS
...