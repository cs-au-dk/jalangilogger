function consoleLog(text) {
    if (console) {
        console.log(text);
    }
}
(function (sandbox) {
    var notifyExit = true;
    var entryIndex = 0;
    var started = false;

    function closeWindowSoon(message) {
        consoleLog(message);
        consoleLog("Closing window down in 5 seconds...");
        setTimeout(function () {
            notifyExit = false;
            window.close()
        }, 5 * 1000);
    }

    function xmlhttp_send(xmlhttp, string) {
        try {
            xmlhttp.send(string);
        } catch (e) {
            closeWindowSoon("Could not send http-request (" + e.message + "). Server has probably been shut down.");
        }
    }

    var natives = {
        Array: {
            forEach: Array.prototype.forEach,
            push: Array.prototype.push,
            slice: Array.prototype.slice
        },
        JSON: {
            ref: JSON,
            stringify: JSON.stringify
        },
        Map: {
            ref: Map,
            forEach: Map.prototype.forEach,
            has: Map.prototype.has,
            set: Map.prototype.set
        },
        String: {
            slice: String.prototype.slice
        }
    };

    function getParameterByName(name, url) {
        if (!url) url = window.location.href;
        name = name.replace(/[\[\]]/g, "\\$&");
        var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
            results = regex.exec(url);
        if (!results) return null;
        if (!results[2]) return '';
        return decodeURIComponent(results[2].replace(/\+/g, " "));
    };

    function getFullLocation(sid, iid) {
        var location = sandbox.iidToLocation(sid, iid);
        if (location === undefined || location === "undefined" || location.startsWith("(eval")) {
            return undefined;
        }
        location = natives.String.slice.call(location, 1, location.length - 1);
        var components = location.split(":");
        var fileName = natives.Array.slice.call(components, 0, components.length - 4).join(':');
        var lineNumber = components[components.length - 4];
        var columnNumber = components[components.length - 3];
        if (typeof lineNumber === 'string' && lineNumber.indexOf('iid') === 0) {
            lineNumber = -1;
            columnNumber = -1;
        }
        return {fileName: fileName, lineNumber: lineNumber, columnNumber: columnNumber};
    }

    var env = (function setupMode() {
        var env = {
            // invoked on J$:functionEnter and J$:conditional: should be enough to avoid infinite executions
            terminator: function () {
            } // default: usually the process is terminated externally.
        };
        var isNode = typeof require === 'function' && typeof require('fs') === 'object';
        var isNashorn = typeof Java === 'object' && typeof Java.type === 'function';
        var isBrowser = typeof window !== 'undefined';
        var isProtractor = isBrowser && getParameterByName("IS_PROTRACTOR");
        env.isNewDriver = isBrowser && getParameterByName("new") === "yes";

        if (isBrowser || isNode) {
            env.makeMap = function () {
                return new natives.Map.ref();
            };
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

        if (env.isNewDriver) {
            var loggedEntriesMap = env.makeMap();
            window.J$.logEntries = [];
            env.log = function (entry) {
                natives.Array.push.call(window.J$.logEntries, entry);
            };
            window.alert = function (x){consoleLog("ALERT(" + x + ")")};
        }
        if (isBrowser && isProtractor) {
            var loggedEntriesMap = env.makeMap();
            window.logEntries = [];
            env.log = function (entry) {
                natives.Array.push.call(window.logEntries, entry);
            };
        }

        /**
         * It is hard to determine when a browser session has ended without manual interaction.
         * This method heuristically ends the browser session based on various timeout mechanisms.
         */
        function makeBrowserSessionTerminators() {
            makeHardTerminator(getParameterByName("hardTimeLimit"));
            makeSoftTerminator(getParameterByName("softTimeLimit"));
            var ENABLE_AUTO_TERMINATOR = false; // TODO this is only a good idea for browser applications that does not require interaction!
            if (ENABLE_AUTO_TERMINATOR) {
                makeAutoTerminator(env.terminator);
            }

            function makeHardTerminator(limit) {
                var hardTimelimit = Number.parseInt(limit) || 0;
                var hardTimelimit_ms = hardTimelimit * 1000;
                if (hardTimelimit_ms > 0) {
                    // handle infinite, synchronous executions
                    var currentTime = +(new Date());
                    var hardEndTime = currentTime + hardTimelimit_ms;
                    env.terminator = function () {
                        var currentTime = +(new Date());
                        if (currentTime > hardEndTime) {
                            sendLoggedEntries(stopBrowserInteraction);
                            if(!env.isNewDriver)
                                throw new Error("hard terminator"); // should make the browser responsive
                            else {
                                if(!this.emitted) {
                                    this.emitted = true;
                                    consoleLog("Hard terminator will terminate the logging, consider reviewing the termination policy to speed-up the termination of this logging sesssion");
                                }
                            }
                        }
                    };
                }
            }

            function makeSoftTerminator(limit) {
                var softTimelimit = Number.parseInt(limit) || 0;
                var softTimelimit_ms = softTimelimit * 1000;
                if (softTimelimit_ms > 0) {
                    // handle crashes or finite executions
                    window.onload = function () {
                        setTimeout(function () {
                            sendLoggedEntries(stopBrowserInteraction);
                        }, softTimelimit_ms)
                    };
                }
            }


            function makeAutoTerminator(oldTerminator) {
                var leastNonUserEventListenerExecutionsInARow = 0;
                env.terminator = function () {
                    oldTerminator && oldTerminator();
                    leastNonUserEventListenerExecutionsInARow = 0;
                };
                function register() {
                    window.setTimeout(function () {
                        leastNonUserEventListenerExecutionsInARow++;
                        if (leastNonUserEventListenerExecutionsInARow > 10) {
                            sendLoggedEntries(stopBrowserInteraction);
                        }
                        register(); // not using setInterval. This implementation ensures the checks are done with delay.
                    }, 250);
                }

                register();
            }

        }

        if (isBrowser && !isProtractor) {
            var sendEntries = true;
            var entriesToSend = [];
            var numberOfEntriesToSendEachTime = 10000;
            var loggedEntriesMap = env.makeMap();
            var lastLogTime = new Date().getTime();

            makeBrowserSessionTerminators();

            function stopBrowserInteraction() {
                if (!sendEntries) //the data has already been sent
                    return;

                if(!env.isNewDriver) {
                    var xmlhttp = new XMLHttpRequest();
                    xmlhttp.open("POST", "/logger-server-api/done", false);
                    try {
                        xmlhttp_send(xmlhttp);
                    } finally {
                        sendEntries = false;
                        closeWindowSoon("Closing window since we are stopping the analysis");
                    }
                }
            }

            function sendLoggedEntries(callback) {
                if(!env.isNewDriver) {
                    if (entriesToSend.length === 0) {
                        return;
                    }
                    var xmlhttp = new XMLHttpRequest();
                    if (callback) {
                        xmlhttp.onreadystatechange = function () {
                            if (xmlhttp.readyState == 4) {
                                callback();
                            }
                        };
                    }
                    xmlhttp.open("POST", "/logger-server-api/sendEntries", true);
                    xmlhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
                    var entries = encodeURIComponent(JSON.stringify(entriesToSend));
                    try {
                        xmlhttp_send(xmlhttp, "entries=" + entries);
                    } catch (e) {
                        callback();
                    }
                    entriesToSend = [];
                } else {

                    if(typeof window.J$.logEntries === 'undefined') {
                        window.J$.logEntries = [];
                    }

                    for(var i in entriesToSend) {
                        window.J$.logEntries.push(entriesToSend[i]);
                    }

                    entriesToSend = [];

                    if (callback === stopBrowserInteraction) {
                        window.J$.stopBrowserInteraction = true;
                    }
                }
            }

            window.onkeyup = function (event) {
                if (event.keyCode == 80) {
                    sendLoggedEntries(stopBrowserInteraction);
                }
            };

            function shouldSendEntry(entry) {
                if (entry in loggedEntriesMap)
                    return false;
                else
                    loggedEntriesMap[entry] = 1;
                return true;
            };

            env.log = function (entry) {
                if (!sendEntries || !shouldSendEntry(natives.JSON.stringify.call(natives.JSON.ref, entry)))
                    return;

                natives.Array.push.call(entriesToSend, entry);
                var currentTime = new Date().getTime();
                var tooMuchTimeHasPassedSinceLastLog = (currentTime - lastLogTime) > 1000;
                if (entriesToSend.length >= numberOfEntriesToSendEachTime || tooMuchTimeHasPassedSinceLastLog) {
                    sendLoggedEntries();
                }
                lastLogTime = new Date().getTime();
            };

            window.onbeforeunload = function () {
                if (notifyExit) {
                    return "Are you sure you want to navigate away?";
                }
            };

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
                        natives.Array.push.call(preambles, preamble);
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
                var entryString = natives.JSON.stringify(entry);

                if (!(natives.Map.has.call(loggedEntriesMap, entryString))) {
                    env.appendStringToFile(LOG_FILE_NAME, entryString + "\n");
                    natives.Map.set.call(loggedEntriesMap, entryString, 1);
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
                if(arguments.length > 1) {
                    var min = 1;
                    var max = arguments.length - 1;
                    return arguments[Math.floor(Math.random() * (max - min + 1)) + min];
                }
                else {
                    switch (s) {
                        case "AnyStr":
                            return "TAJS_make('AnyStr');"
                        case "AnyNum":
                            return 42;
                        case "AnyBool":
                            return true;
                        case "AnyStrUInt":
                            return '0';
                        case "AnyStrNotUInt":
                            return "TAJS_make('AnyStrNotUInt')";
                        case "AnyStrIdent":
                            return "AnyStrIdent";
                    }
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
            TAJS_makeExcludedStrings = function () {
                return "TAJS_makeExcludedStrings(" + Array.prototype.join.call(arguments, ", ") + ")";
            };
            TAJS_newArray = function () {
                return [];
            };
            TAJS_load = function () {

            };
            TAJS_record = function () {

            };
            TAJS_restrictTo = function () {

            };
            TAJS_addTaint = function (v) {
                return v;
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
                if (!isNonPrimitive || natives.Map.has.call(map, value)) {
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
            natives.Array.forEach.call(commonGlobalNames, function (ID) {
                var globalProperty = global[ID];
                if (globalProperty) {
                    registerBuiltin(ID, globalProperty);
                }
            });
            // add non-primitives from objects with constructor-like names, and their prototypes
            natives.Map.forEach.call(map, function (ID, value) {
                if (typeof ID !== 'string') {
                    throw new Error('Invalid ID: ' + ID);
                }
                var isConstructorLike = typeof value === 'function' && ID[0] === ID[0].toUpperCase();
                if (!isConstructorLike) {
                    return;
                }
                natives.Array.forEach.call(Object.getOwnPropertyNames(value), function (valuePropertyName) {
                    var propertyValueID = ID + "." + valuePropertyName;
                    if (isBanned(propertyValueID, value, valuePropertyName)) {
                        return;
                    }
                    var propertyValue = value[valuePropertyName];
                    registerBuiltin(propertyValueID, propertyValue);
                });
                var prototype = value.prototype;
                if (prototype) {
                    natives.Array.forEach.call(Object.getOwnPropertyNames(prototype), function (prototypePropertyName) {
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

            if (typeof val === 'symbol') {
                return {objectKind: "other-symbol", toStringValue: val.toString()};
            }
            return {objectKind: "other"};
        }

        function makeValueForObject(val) {
            return {valueKind: "abstract-object", value: describeObject(val)};
        }

        function makeValueForNonStringPrimitive(val) {
            return {valueKind: "abstract-primitive", value: describeNonStringPrimitive(val)};
        }

        function makeValue(val, forbidStringAbstraction) {
            if (typeof val === "function" || (typeof val === "object" && val !== null) || typeof val === 'symbol') {
                return makeValueForObject(val);
            }
            if (typeof val === "string") {
                return makeValueForString(val, forbidStringAbstraction);
            }
            if (typeof val === 'undefined' && val !== undefined) {
                return makeValueForObject(val); // legacy objects like `document.all`: https://bugs.chromium.org/p/chromium/issues/detail?id=567998
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
            return "TYPE_<" + t + ">";
        }

        function makeArrayValue(args, forbidStringAbstraction) {
            var result = [];
            for (var i = 0; i < args.length; i++)
                natives.Array.push.call(result, makeValue(args[i], forbidStringAbstraction));
            return result;
        }

        this.getOutputAsString = function (from, to) {
            var existingArrayToJSON = Array.prototype.toJSON;
            Array.prototype.toJSON = null;

            var entries = window.logEntries;
            if (typeof from === 'number' && typeof to === 'number') {
                entries = natives.Array.slice.call(entries, from, to);
            }

            var result = natives.JSON.stringify.call(natives.JSON.ref, entries);

            Array.prototype.toJSON = existingArrayToJSON;

            return result;
        };

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
            var isUserConstructorCall = isConstructor && (functionIid !== undefined);
            if (isUserConstructorCall) {
                nextConstructorCallCallSiteGIID = J$.getGlobalIID(iid);
            }
            logEntry(iid, {
                entryKind: "call",
                function: makeValue(f),
                base: makeValue(base),
                arguments: makeArrayValue(args, f === eval || f === Function /* forbid abstraction of strings to the dynamic code functions: Function & eval */)
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

        this.scriptEnter = function (iid, instrumentedFileName, originalFileName) {
            if (!started) {
                consoleLog("Starting analysis at: " + instrumentedFileName);
                started = true;
                if (typeof XMLHttpRequest !== "undefined" && !env.isNewDriver) {
                    var xmlhttp = new XMLHttpRequest();
                    xmlhttp.open("POST", "/logger-server-api/started", false);
                    xmlhttp_send(xmlhttp);
                }
            }
        };

        this.functionEnter = function (iid, f, dis, args) {
            env.terminator();
            if (nextConstructorCallCallSiteGIID) {
                var sid_iid = nextConstructorCallCallSiteGIID.split(":");
                registerAllocation(sid_iid[1], dis, sid_iid[0]);
                nextConstructorCallCallSiteGIID = undefined;
            }
            logEntry(iid, {entryKind: "function-entry", base: makeValue(dis), arguments: makeArrayValue(args)});
        };

        this.conditional = function () {
            env.terminator();
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

        /* called when loaded using the jalangi2 version that is used by tracifier  */
        this.onText = function () {};

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

        function makeValueForString(val, forbidAbstraction) {
            var limit = 50;
            if (!forbidAbstraction && val.length > limit) {
                return {valueKind: "prefix-string", value: val.substring(0, limit)};
            }
            return {valueKind: "concrete-string", value: val};
        }

        function makeValueForPropertyName(val) {
            if (typeof val == "string") {
                return makeValueForString(val, true);
            } else {
                return makeValue(val);
            }
        }
    }

    sandbox.analysis = new MyAnalysis();

    sandbox.analysis.startTag =
        sandbox.analysis.endTag =
            sandbox.analysis.documentType =
                sandbox.analysis.removeInjectedScripts =
                    function () {};
})(J$);
