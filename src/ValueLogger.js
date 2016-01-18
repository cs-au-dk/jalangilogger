(function (sandbox) {
    var log /* the function to store log entries with */;

	function getFullLocation(iid){
		var location = sandbox.iidToLocation(sandbox.getGlobalIID(iid));
		location = location.slice(1, location.length - 1);
		var components = location.split(":");
		var lineNumber = components[1];
		var columnNumber = components[2];
		if(typeof lineNumber === 'string' && lineNumber.indexOf('iid') === 0){
			lineNumber = -1;
			columnNumber = -1;
		}
		return {fileName: components[0], lineNumber: lineNumber, columnNumber: columnNumber};
	}

	(function setupMode() {
		if (typeof window !== 'undefined') {

            var sendEntries = true;
            var entriesToSend = [];
            var numberOfEntriesToSendEachTime = 10000;
            var loggedEntriesMap = new Map;
            
            function sendLoggedEntries() {
                var xmlhttp = new XMLHttpRequest();
                xmlhttp.open("POST", "http://127.0.0.1:3000/sendEntries", true);
                xmlhttp.setRequestHeader("Content-type","application/x-www-form-urlencoded");
                xmlhttp.send( "entries=" + entriesToSend);
            }

            window.onkeyup = function(event){
                if(event.keyCode == 80){
                    sendLoggedEntries();
                    if(!sendEntries) //the data has already been sent
                        return;

                    var urlLocation = location.pathname;
                    var htmlFileName = urlLocation.substr(urlLocation.indexOf("/instrumentedHtmlFiles/") + "/instrumentedHtmlFiles/".length);
                    var logFileName = ""; //Jalangi makes a folder with the name of the html file, which we do not want in this path
                    var directoriesInHTMLFileName = htmlFileName.split("/");
                    for (var i = 0; i < directoriesInHTMLFileName.length - 1; i++){
                        if(directoriesInHTMLFileName[i].indexOf(".html") != -1)
                            continue;
                        logFileName += directoriesInHTMLFileName[i] + "/"
                    }
                    logFileName += directoriesInHTMLFileName[directoriesInHTMLFileName.length - 1];
                    logFileName = logFileName.substring(0, logFileName.length - 4) + "log";

                    var xmlhttp = new XMLHttpRequest();
                    xmlhttp.open("POST", "http://127.0.0.1:3000/printToFile", true);
                    xmlhttp.setRequestHeader("Content-type","application/x-www-form-urlencoded");
                    xmlhttp.send( "fileName=" + logFileName);
                    sendEntries = false;
                    console.log("Send printToFileCommand")
                }
            };
            shouldSendEntry = function (entry){
            	if(entry in loggedEntriesMap){		
            		return false;		
            	}		
            	loggedEntriesMap[entry] = 1		
            	return true;
            };
            log = function (iid, entry) {
                entry.sourceLocation = getFullLocation(iid);
                if (!sendEntries || !shouldSendEntry(JSON.stringify(entry)))
                    return;

                entriesToSend.push(JSON.stringify(entry) + "\n");
                if (entriesToSend.length >= numberOfEntriesToSendEachTime) {
                    sendLoggedEntries();
                }
            }
        } else {
            log = function (iid, entry) {
                entry.sourceLocation = getFullLocation(iid);
                console.log(JSON.stringify(entry));
            }
        }
    })();


    var preambles = {
        TAJS: function () {
            TAJS_dumpValue = function (s) {
            };
            TAJS_dumpPrototype = function () {
            };
            TAJS_dumpObject = function () {
            };
            TAJS_dumpState = function () {
            };
            TAJS_dumpModifiedState = function () {
            };
            TAJS_dumpAttributes = function () {
            };
            TAJS_dumpExp = function () {
            };
            TAJS_dumpNF = function () {
            };
            TAJS_conversionToPrimitive = function () {
            };
            TAJS_getUIEvent = function () {
            };
            TAJS_getDocumentEvent = function () {
            };
            TAJS_getMouseEvent = function () {
            };
            TAJS_getKeyboardEvent = function () {
            };
            TAJS_getEventListener = function () {
            };
            TAJS_getWheelEvent = function () {
            };
            TAJS_getAjaxEvent = function () {
            };
            TAJS_addContextSensitivity = function () {
            };
            TAJS_assert = function (a) {
            };
            TAJS_newObject = function () {
            };
        },
        print: function () {
            print = function () {
            }; //google/cryptobench test
        },
        asserts: function () {
            assertTrue = function () {
            };
            assertEquals = function () {
            };
            assertThrows = function () {
            };
            assertHasOwnProperties = function () {
            };
            assertArrayEquals = function () {
            };
            assertFalse = function () {
            };
        }
    };
    preambles.asserts();
    preambles.TAJS();
    preambles.print();


	function MyAnalysis () {

        var allocationSites = new Map/*<Object, IID>*/();
        var builtins = makeBuiltinsMap();
		var nextConstructorCallCallSiteIID = false;
		var nativeCall = Function.prototype.call;
		var nativeApply = Function.prototype.apply;
		var nativeSetTimeout = setTimeout;
		var nativeSetInterval = setInterval;

        /**
         * Builds a map from some builtin objects to their canonical path
         */
        function makeBuiltinsMap(){
            var map = new Map/*<Object, Path>*/();

            function register(k, v) {
                if (k === null
                    || (typeof k !== 'object' && typeof k !== 'function')
                    || map.has(k)) {
                    return;
                }
                map.set(k, v);
            }

			var roots = {
				Object: Object,
				Function: Function,
				Array: Array,
				RegExp: RegExp,
				Date: Date,
				Math: Math,
				'Object.prototype': Object.prototype,
				'Function.prototype': Function.prototype,
				'Array.prototype': Array.prototype,
				'RegExp.prototype': RegExp,
				'Date.prototype': Date.prototype
			};

		 	var global = Function('return this')();
			register(global, '<the global object>');
			register(Object, 'Object');
			register(Function, 'Function');
			register(Array, 'Array');
			register(RegExp, 'RegExp');
			register(Date, 'Date');
			register(Math, 'Math');
			for (var prefix in roots) {
				var root = roots[prefix];
				Object.getOwnPropertyNames(root).forEach(
					function (propertyName) {
						if(prefix === "Function.prototype" && (propertyName === "arguments" || propertyName === "caller")) {
							return;
						}
						var propertyValue = root[propertyName];
						if (typeof propertyValue === 'function' || typeof propertyValue === 'object') {
							var path = prefix + '.' + propertyName; //console.log(prefix + "   " + propertyName)
							register(propertyValue, path);
						}
					}
				);
			}
            return map;
        }

        function describeObject(val){
            if(allocationSites.has(val)){
                return {objectKind: "allocation-site", allocationSite: getFullLocation(allocationSites.get(val))};
            }

            if(builtins.has(val)){
                return {objectKind: "builtin", canonicalName: builtins.get(val)};
            }

            return {objectKind: "other"};
        }

		function p(val) {
			if (typeof val === "function" || (typeof val === "object" && val !== null)) {
				return {valueKind: "abstract-object", value: describeObject(val)};
			}
			return {valueKind: "abstract-primitive", value: describePrimitive(val)};
		}

		function describePrimitive(val){
			var t = typeof val;
			if (t == "undefined" || t == "boolean" || val === null)
				return val + '';
			if (t == "number") {
				if (val == 0 || val == 1)
					return val + '';
				if (isNaN(val))
					return "NUM_NAN";
				if (!isFinite(val))
					return "NUM_INF";
				if (val >= 0 && val <= 4294967295 && (val % 1) == 0)
					return "NUM_UINT";
				return "NUM_OTHER";
			}
			if (t == "string") {
				if(val.indexOf(' ') === -1 && val.indexOf('\n') === -1 && val.indexOf('\t') === -1 && val !== '' /* avoid toNumber string-manipulations */) {
					var n = +val;
					if(val === 'NaN' || val.match("^-?[0-9]+[e][-|+][0-9]+$") /* exponentials should be logged to STR_OTHERNUM */){
						return "STR_OTHERNUM";
					}else if (!isNaN(n) && val.match("^[0-9]+$")) {
						if (n >= 0 && n <= 4294967295 && (n % 1) == 0) {
							return "STR_UINT";
						}
						return "STR_OTHERNUM";
					} else if(val.match("^-?[0-9]*\\.[0-9]+[e][-|+][0-9]+$")
							|| val.match("^-?[0-9]*\\.[0-9]+$")
							|| val.match("^-?[0-9]+\\.[0-9]*$")
							|| val.match("^-[0-9]+$")
							|| val === "Infinity"
							|| val === "-Infinity"){
						return "STR_OTHERNUM";
					}
				}
				if(val.match("^[_$a-zA-Z\xA0-\uFFFF][_$a-zA-Z0-9\xA0-\uFFFF]*$") && !isReservedName(val)){
					//identifiers - Not precise - See http://stackoverflow.com/questions/2008279/validate-a-javascript-function-name/2008444#2008444
					return "STR_IDENTIFIER";
				}
				if(val.match("^[_$a-zA-Z0-9\xA0-\uFFFF]*$")){
					//identifierParts - Not precise - See http://stackoverflow.com/questions/2008279/validate-a-javascript-function-name/2008444#2008444
					return "STR_IDENTIFIERPARTS";
				}
				//if()
				//return "STR_JSON" //TODO STR_JSON abstraction

				return "STR_OTHER"

			}
			return "???";
		}
		function pa(args) {
			var result = [];
			for (var i = 0; i < args.length; i++)
				result.push(p(args[i]));
			return result;
		}

		this.read = function(iid, name, val, isGlobal, isScriptLocal) {
			log(iid, {entryKind: "read-variable", name: s(name), value: p(val)});
		};

		this.write = function(iid, name, val, lhs, isGlobal, isScriptLocal) {
			log(iid, {entryKind: "write-variable", name: s(name), value: p(val)});
		};

		this.getField = function(iid, base, offset, val, isComputed, isOpAssign, isMethodCall) {
			log(iid, {entryKind: "read-property", name: s(offset), value: p(val)});
		};

		this.putField = function(iid, base, offset, val, isComputed, isOpAssign) {
			log(iid, {entryKind: "write-property", name: s(offset), value: p(val)});
		};

		this.invokeFunPre = function(iid, f, base, args, isConstructor, isMethod, functionIid) {
			if(typeof f !== "function"){ //about to crash
				return;
			}
			var isUserConstructorCall = isConstructor && (functionIid !== undefined);
            if(isUserConstructorCall){
				nextConstructorCallCallSiteIID = iid;
			}
    		log(iid, {entryKind: "call", function: p(f), base: p(base), arguments: pa(args)});

			if((f === nativeCall || f === nativeApply) && f){
				// if Function.prototype.apply/call is used, register an extra call entry
				var newF = base;
				var newBase = args[0];
				var newArgs = f === nativeCall? Array.prototype.slice.call(args, 1, args.length): args[1] || [];
                this.invokeFunPre(iid, newF, newBase, newArgs, false, newBase !== undefined, allocationSites.get(newF));
			} else if ((nativeSetTimeout === f  || nativeSetInterval === f) && (typeof args[0] !== 'function')) {
				var args2 = [];
				for(var i = 0; i < args.length; i++){
					args2[i] = args[i];
				}
				args2[0] = J$.instrumentEvalCode(args[0], iid);
				return {f: f, base: base, args: args2, skip: false};
			}


		};

        this.invokeFun = function (iid, f, base, args, result, isConstructor, isMethod, functionIid) {
			var isBuiltinConstructorCall = isConstructor && !allocationSites.has(f) /* ex. new String()*/;
            if(isBuiltinConstructorCall){
				registerAllocation(iid, result);
            }
        };

        this.functionEnter = function(iid, f, dis, args) {
			if(nextConstructorCallCallSiteIID){
				registerAllocation(nextConstructorCallCallSiteIID, dis);
				nextConstructorCallCallSiteIID = undefined;
			}
            log(iid, {entryKind: "function-entry", base: p(dis), arguments: pa(args)});
		};

		this.functionExit = function(iid, returnVal, wrappedExceptionVal) {
			var entry = {entryKind: "function-exit"};
			if(wrappedExceptionVal){
				entry.exceptionValue = p(wrappedExceptionVal.exception);
			}else{
				entry.returnValue = p(returnVal);
			}
            log(iid, entry);
		};

        this.literal = function (iid, val, hasGetterSetter) {
            registerAllocation(iid, val);
        };

		this.instrumentCodePre = function (iid, code) {
			log(iid, {entryKind: 'dynamic-code', code: code + ''});
		};

        function registerAllocation(iid, val){
            if (typeof val === 'function' || typeof val === 'object') {
                allocationSites.set(val, iid);
            }
        }

		function s(val) {
			if (typeof val == "string") {
				return {valueKind: "concrete-string", value: val};
			} else {
				return p(val);
			}
		}

		/**
		 * Checks whether the given string is a reserved name.
		 */
		function isReservedName(s) {
			if (s == "")
				return false;
			switch (s.charAt(0)) {
			case 'a':
				return s == "abstract";
			case 'b':
				return s == "boolean" || s == "break" || s == "byte";
			case 'c':
				return s == "case" || s == "catch" || s == "char" || s == "class"
				|| s == "const" || s == "continue";
			case 'd':
				return s == "debugger" || s == "default" || s == "delete" || s == "do"
				|| s == "double";
			case 'e':
				return s == "else" || s == "enum" || s == "export" || s == "extends";
			case 'f':
				return s == "false" || s == "final" || s == "finally" || s == "float"
				|| s == "for" || s == "function";
			case 'g':
				return s == "goto";
			case 'i':
				return s == "if" || s == "implements" || s == "import" || s == "in"
				|| s == "instanceof" || s == "int" || s == "interface";
			case 'l':
				return s == "long";
			case 'n':
				return s == "native" || s == "new" || s == "null";
			case 'p':
				return s == "package" || s == "private" || s == "protected" || s == "public";
			case 'r':
				return s == "return";
			case 's':
				return s == "short" || s == "static" || s == "super" || s == "switch"
				|| s == "synchronized";
			case 't':
				return s == "this" || s == "throw" || s == "throws" || s == "transient"
				|| s == "true" || s == "try" || s == "typeof";
			case 'v':
				return s == "var" || s == "void" || s == "volatile";
			case 'w':
				return s == "while" || s == "with";
			default:
				return false;
			}
		}

	}
	sandbox.analysis = new MyAnalysis();
})(J$);


