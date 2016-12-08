(function (sandbox) {
    var notifyExit = true;
    var entryIndex = 0;

    function getFullLocation(sid, iid) {
        var location = sandbox.iidToLocation(sid, iid);
        if (location === undefined || location === "undefined" || location.startsWith("(eval")) {
            return undefined;
        }
        location = location.slice(1, location.length - 1);
        var components = location.split(":");
        var fileName = components.slice(0, components.length - 4).join(':');
        var lineNumber = components[components.length - 4];
        var columnNumber = components[components.length - 3];
        if (typeof lineNumber === 'string' && lineNumber.indexOf('iid') === 0) {
            lineNumber = -1;
            columnNumber = -1;
        }
        return {fileName: fileName, lineNumber: lineNumber, columnNumber: columnNumber};
    }

    var env = (function setupMode() {
        var env = {};
        var isNode = typeof require === 'function' && typeof require('fs') === 'object';
        var isNashorn = typeof Java === 'object' && typeof Java.type === 'function';
        var isBrowser = typeof window !== 'undefined';
        var isProtractor = isBrowser && window.location.href.indexOf("file://") != 0

        if (isBrowser || isNode) {
            env.makeMap = function () {
                return new Map();
            },
                env.setTimeout = setTimeout;
        }
        if (isNode) {
            env.appendStringToFile = function (string, file) {
                require('fs').appendFileSync(string, file);
            };
            env.readStringFromFile = function (file) {
                return require('fs').readFileSync(file, 'utf-8');
            };
            env.arguments = process.argv;
        }
        if (isNashorn) {
            env.appendStringToFile = function (file, string) {
                var FileWriter = Java.type("java.io.FileWriter");
                var fw = new FileWriter(file, true);
                fw.write(string);
                fw.close();
            };
            env.readStringFromFile = function (file) {
                var Files = Java.type("java.nio.Files");
                var Paths = Java.type("java.nio.Path");
                var String = Java.type("java.lang.String");
                var encoded = Files.readAllBytes(Paths.get(path));
                return new String(encoded, "UTF-8");
            };
            env.arguments = (eval /* global */)(arguments);
            env.makeMap = function () {
                var HashMap = Java.type("java.util.HashMap");
                var ArrayList = Java.type("java.util.ArrayList");
                var map = new HashMap();
                return {
                    set: function (k, v) {
                        map.put(k, v);
                    },
                    has: function (k) {
                        return map.containsKey(k);
                    },
                    get: function (k) {
                        return map.get(k);
                    },
                    forEach: function (cb) {
                        new ArrayList(map.entrySet()).forEach(function (e) {
                            cb(e.getValue(), e.getKey())
                        });
                        return undefined;
                    }
                };
            },
                env.setTimeout = function (f) {
                    var Timer = Java.type('java.util.Timer');
                    var timer = new Timer('jsEventLoop', false);
                    timer.schedule(f, 0);
                }
        }

        if (isBrowser && isProtractor) {
            var loggedEntriesMap = env.makeMap();
            window.logEntries = [];
            env.log = function (entry) {
                window.logEntries.push(entry);
            };
        }

        if (isBrowser && !isProtractor) {
            var sendEntries = true;
            var entriesToSend = [];
            var numberOfEntriesToSendEachTime = 10000;
            var loggedEntriesMap = env.makeMap();

            function stopBrowserInteraction() {
                if (!sendEntries) //the data has already been sent
                    return;

                var urlLocation = location.pathname;
                var htmlFileName = urlLocation.substr(urlLocation.indexOf("/instrumentedHtmlFiles/") + "/instrumentedHtmlFiles/".length);
                var logFileName = ""; //Jalangi makes a folder with the name of the html file, which we do not want in this path
                var directoriesInHTMLFileName = htmlFileName.split("/");
                for (var i = 0; i < directoriesInHTMLFileName.length - 1; i++) {
                    if (directoriesInHTMLFileName[i].indexOf(".html") != -1)
                        continue;
                    logFileName += directoriesInHTMLFileName[i] + "/"
                }
                logFileName += directoriesInHTMLFileName[directoriesInHTMLFileName.length - 1];
                logFileName = logFileName.substring(0, logFileName.length - 4) + "log";

                var xmlhttp = new XMLHttpRequest();
                xmlhttp.open("POST", "http://127.0.0.1:3000/printToFile", false);
                xmlhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
                xmlhttp.send("fileName=" + logFileName);
                sendEntries = false;
                console.log("Send printToFileCommand");
                notifyExit = false;
                close();
            }

            function sendLoggedEntries(callback) {
                var xmlhttp = new XMLHttpRequest();
                if (callback) {
                    xmlhttp.onreadystatechange = function () {
                        if (xmlhttp.readyState == 4 && xmlhttp.status == 204) {
                            callback();
                        }
                    };
                }
                xmlhttp.open("POST", "http://127.0.0.1:3000/sendEntries", true);
                xmlhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
                var entries = encodeURIComponent(JSON.stringify(entriesToSend));
                xmlhttp.send("entries=" + entries);
                entriesToSend = [];
            }

            window.onkeyup = function (event) {
                if (event.keyCode == 80) {
                    sendLoggedEntries(stopBrowserInteraction);
                }
            };
            var enableAutoClosingAfterLoading = false;
            if (enableAutoClosingAfterLoading) {
                window.onload = function () {
                    setTimeout(function () {
                        sendLoggedEntries(stopBrowserInteraction);
                    }, 3000)
                };
            }

            function shouldSendEntry(entry) {
                if (entry in loggedEntriesMap)
                    return false;
                else
                    loggedEntriesMap[entry] = 1;
                return true;
            };

            env.log = function (entry) {
                if (!sendEntries || !shouldSendEntry(JSON.stringify(entry)))
                    return;

                entriesToSend.push(entry);
                if (entriesToSend.length >= numberOfEntriesToSendEachTime) {
                    sendLoggedEntries();
                }
            };

            window.onbeforeunload = function () {
                if (notifyExit) {
                    return "Are you sure you want to navigate away?";
                }
            }
        } else if (isNashorn || isNode) {
            function loadPreambles(preambles) {
                // eval evaluates in the global context if it's referenced by a variable
                // which is not named eval.
                var evalGlobal = eval;
                for (var i = 0; i < preambles.length; i++) {
                    var fileContent = env.readStringFromFile(preambles[i]);
                    evalGlobal(fileContent);
                }
            }

            function extractPreambles() {
                var args = env.arguments;
                var preambles = [];

                for (var i = 0; i < args.length; i++) {
                    var arg = args[i];
                    if (isPreambleOption(arg)) {
                        var preamble = args[++i];
                        preambles.push(preamble);
                    }
                }
                return preambles;

                function isPreambleOption(arg) {
                    return arg === "--preamble";
                }
            }

            var LOG_FILE_NAME = 'NEW_LOG_FILE.log';
            env.appendStringToFile(LOG_FILE_NAME, "");
            var preambles = extractPreambles();
            loadPreambles(preambles);
            var loggedEntriesMap = env.makeMap();
            env.log = function (entry) {
                var entryString = JSON.stringify(entry);

                if (!(loggedEntriesMap.has(entryString))) {
                    env.appendStringToFile(LOG_FILE_NAME, entryString + "\n");
                    loggedEntriesMap.set(entryString, 1);
                }
            }
        }
        return env;
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
                return {};
            };
            TAJS_join = function () {
                return arguments[0];
            };
            TAJS_assertEquals = function () {
            };
            TAJS_make = function (s) {
                switch (s) {
                    case "AnyStr":
                        return "TAJS_make('AnyStr');"
                    case "AnyNum":
                        return 42;
                    case "AnyBool":
                        return true;
                }
                return;
            };
            TAJS_asyncListen = function (f) {
                env.setTimeout(f)
            };
            TAJS_makeContextSensitive = function () {
            };
            TAJS_split = function () {
            };
            TAJS_makeComplementaryString = function () {
                return "TAJS_makeComplementaryString(" + Array.prototype.join.call(arguments, ", ") + ")";
            };
            TAJS_newArray = function () {
                return [];
            };
            TAJS_load = function () {

            };
            TAJS_record = function () {

            }
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

    function logEntry(iid, entry) {
        entry.sourceLocation = getFullLocation(sandbox.sid, iid);
        if (entry.sourceLocation == undefined) {
            return;
        }
        entry.index = entryIndex++;
        entry.index = -1; // TODO support indexes on entries
        env.log(entry);
    }

    function MyAnalysis() {

        var allocationSites = env.makeMap/*<Object, {sid: SID, iid: IID}>*/();
        var builtins = makeBuiltinsMap();
        var nextConstructorCallCallSiteGIID = false;
        var nativeCall = Function.prototype.call;
        var nativeApply = Function.prototype.apply;
        var nativeSetTimeout = typeof setTimeout === 'undefined' ? undefined : setTimeout;
        var nativeSetInterval = typeof setInterval === 'undefined' ? undefined : setInterval;

        /**
         * Builds a map from some builtin objects to their canonical path
         */
        function makeBuiltinsMap() {
            var map = env.makeMap/*<Object, Path>*/();

            function registerBuiltin(ID, value) {
                if (typeof ID !== 'string') {
                    throw new Error('Invalid ID: ' + ID);
                }
                var isNonPrimitive = typeof value === 'function' || (typeof value === 'object' && value !== null);
                if (!isNonPrimitive || map.has(value)) {
                    return;
                }
                map.set(value, ID);
            }


            var bannedIDs = [
                'Function.prototype.arguments',
                'Function.prototype.callee'
            ];

            function isBanned(ID, obj, prop) {
                var isGetter = Object.getOwnPropertyDescriptor(obj, prop).get !== undefined;
                return bannedIDs.indexOf(ID) !== -1 || isGetter;
            }

            var global = Function('return this')();
            registerBuiltin('<the global object>', global);
            // add selected globals
            var commonGlobalNames = [
                'Array',
                'ArrayBuffer',
                'Boolean',
                'Buffer',
                'DataView',
                'Date',
                'Error',
                'Float32Array',
                'Float64Array',
                'Function',
                'Int16Array',
                'Int32Array',
                'Int8Array',
                'JSON',
                'Map',
                'Math',
                'Number',
                'Object',
                'Promise',
                'RangeError',
                'ReferenceError',
                'RegExp',
                'Set',
                'String',
                'Symbol',
                'SyntaxError',
                'TypeError',
                'URIError',
                'Uint16Array',
                'Uint32Array',
                'Uint8Array',
                'Uint8ClampedArray',
                'WeakMap',
                'WeakSet',
                'console',
                'decodeURI',
                'decodeURIComponent',
                'encodeURI',
                'encodeURIComponent',
                'escape',
                'isFinite',
                'parseFloat',
                'parseInt',
                'unescape'];
            commonGlobalNames.forEach(function (ID) {
                var globalProperty = global[ID];
                if (globalProperty) {
                    registerBuiltin(ID, globalProperty);
                }
            });
            // add non-primitives from objects with constructor-like names, and their prototypes
            map.forEach(function (ID, value) {
                if (typeof ID !== 'string') {
                    throw new Error('Invalid ID: ' + ID);
                }
                var isConstructorLike = typeof value === 'function' && ID[0] === ID[0].toUpperCase();
                if (!isConstructorLike) {
                    return;
                }
                Object.getOwnPropertyNames(value).forEach(function (valuePropertyName) {
                    var propertyValueID = ID + "." + valuePropertyName;
                    if (isBanned(propertyValueID, value, valuePropertyName)) {
                        return;
                    }
                    var propertyValue = value[valuePropertyName];
                    registerBuiltin(propertyValueID, propertyValue);
                });
                var prototype = value.prototype;
                if (prototype) {
                    Object.getOwnPropertyNames(prototype).forEach(function (prototypePropertyName) {
                        var prototypePropertyValueID = ID + ".prototype." + prototypePropertyName;
                        if (isBanned(prototypePropertyValueID, prototype, prototypePropertyName)) {
                            return;
                        }
                        var prototypePropertyValue = prototype[prototypePropertyName];
                        registerBuiltin(prototypePropertyValueID, prototypePropertyValue);
                    });
                }
            });
            return map;
        }

        function describeObject(val) {
            if (allocationSites.has(val)) {
                var allocationSite = allocationSites.get(val);
                var fullLocation = getFullLocation(allocationSite.sid, allocationSite.iid);
                if (fullLocation !== undefined) {
                    return {objectKind: "allocation-site", allocationSite: fullLocation};
                }
            }

            if (builtins.has(val)) {
                return {objectKind: "builtin", canonicalName: builtins.get(val)};
            }

            return {objectKind: "other"};
        }

        function makeValueForObject(val) {
            return {valueKind: "abstract-object", value: describeObject(val)};
        }

        function makeValueForNonStringPrimitive(val) {
            return {valueKind: "abstract-primitive", value: describeNonStringPrimitive(val)};
        }

        function makeValue(val) {
            if (typeof val === "function" || (typeof val === "object" && val !== null)) {
                return makeValueForObject(val);
            }
            if (typeof val === "string") {
                return makeValueForString(val, true);
            }
            return makeValueForNonStringPrimitive(val);
        }

        function describeNonStringPrimitive(val) {
            var t = typeof val;
            if (t == "undefined" || t == "boolean" || val === null)
                return val + '';
            if (t == "number") {
                if (-100 < val && val < 100 && (val % 1) == 0)
                    return val + ''; // small integers
                if (isNaN(val))
                    return "NUM_NAN";
                if (!isFinite(val))
                    return "NUM_INF";
                if (val >= 0 && val <= 4294967295 && (val % 1) == 0)
                    return "NUM_UINT";
                return "NUM_OTHER";
            }
            return "???";
        }

        function makeArrayValue(args) {
            var result = [];
            for (var i = 0; i < args.length; i++)
                result.push(makeValue(args[i]));
            return result;
        }

        this.read = function (iid, name, val, isGlobal, isScriptLocal) {
            logEntry(iid, {entryKind: "read-variable", name: makeValueForPropertyName(name), value: makeValue(val)});
        };

        this.write = function (iid, name, val, lhs, isGlobal, isScriptLocal) {
            logEntry(iid, {entryKind: "write-variable", name: makeValueForPropertyName(name), value: makeValue(val)});
        };

        this.getField = function (iid, base, offset, val, isComputed, isOpAssign, isMethodCall) {
            logEntry(iid, {
                entryKind: "read-property",
                base: makeValue(base),
                name: makeValueForPropertyName(offset),
                value: makeValue(val)
            });
        };

        this.putField = function (iid, base, offset, val, isComputed, isOpAssign) {
            logEntry(iid, {
                entryKind: "write-property",
                base: makeValue(base),
                name: makeValueForPropertyName(offset),
                value: makeValue(val)
            });
        };

        this.invokeFunPre = function (iid, f, base, args, isConstructor, isMethod, functionIid) {
            if (typeof f !== "function") { //about to crash
                return;
            }
            var isUserConstructorCall = isConstructor && (functionIid !== undefined);
            if (isUserConstructorCall) {
                nextConstructorCallCallSiteGIID = J$.getGlobalIID(iid);
            }
            logEntry(iid, {
                entryKind: "call",
                function: makeValue(f),
                base: makeValue(base),
                arguments: makeArrayValue(args)
            });

            if ((f === nativeCall || f === nativeApply) && f) {
                // if Function.prototype.apply/call is used, register an extra call entry
                var newF = base;
                var newBase = args[0];
                var newArgs = f === nativeCall ? Array.prototype.slice.call(args, 1, args.length) : args[1] || [];
                var newFAllocationSite = allocationSites.get(newF);
                this.invokeFunPre(iid, newF, newBase, newArgs, false, newBase !== undefined, newFAllocationSite ? newFAllocationSite.iid : undefined);
            } else if ((nativeSetTimeout === f || nativeSetInterval === f) && (typeof args[0] !== 'function')) {
                var args2 = [];
                for (var i = 0; i < args.length; i++) {
                    args2[i] = args[i];
                }
                args2[0] = J$.instrumentEvalCode(args[0], iid);
                return {f: f, base: base, args: args2, skip: false};
            }


        };

        this.invokeFun = function (iid, f, base, args, result, isConstructor, isMethod, functionIid) {
            var isBuiltinConstructorCall = isConstructor && !allocationSites.has(f) /* ex. new String()*/;
            if (isBuiltinConstructorCall) {
                registerAllocation(iid, result);
            }
        };

        this.functionEnter = function (iid, f, dis, args) {
            if (nextConstructorCallCallSiteGIID) {
                var sid_iid = nextConstructorCallCallSiteGIID.split(":");
                registerAllocation(sid_iid[1], dis, sid_iid[0]);
                nextConstructorCallCallSiteGIID = undefined;
            }
            logEntry(iid, {entryKind: "function-entry", base: makeValue(dis), arguments: makeArrayValue(args)});
        };

        this.functionExit = function (iid, returnVal, wrappedExceptionVal) {
            var entry = {entryKind: "function-exit"};
            if (wrappedExceptionVal) {
                entry.exceptionValue = makeValue(wrappedExceptionVal.exception);
            } else {
                entry.returnValue = makeValue(returnVal);
            }
            logEntry(iid, entry);
        };

        this.literal = function (iid, val, hasGetterSetter) {
            registerAllocation(iid, val);
        };

        this.instrumentCodePre = function (iid, code) {
            logEntry(iid, {entryKind: 'dynamic-code', code: code + ''});
        };

        function registerAllocation(iid, val, sid) {
            if (typeof sid == "undefined") {
                sid = sandbox.sid;
            }
            if (typeof val === 'function' || typeof val === 'object') {
                allocationSites.set(val, {sid: sid, iid: iid});
            }
            if (typeof val === 'function') {
                allocationSites.set(val.prototype, {sid: sid, iid: iid});
            }
        }

        function makeValueForString(val, allowAbstraction) {
            var limit = 50;
            if (allowAbstraction && val.length > limit) {
                return {valueKind: "prefix-string", value: val.substring(0, limit)};
            }
            return {valueKind: "concrete-string", value: val};
        }

        function makeValueForPropertyName(val) {
            if (typeof val == "string") {
                return makeValueForString(val, false);
            } else {
                return makeValue(val);
            }
        }
    }

    sandbox.analysis = new MyAnalysis();
})(J$);