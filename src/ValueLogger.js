
(function (sandbox) {

	function MyAnalysis () {

		var info = {};
        var allocationSites = new Map/*<Object, IID>*/();
        var builtins = makeBuiltinsMap();

        /**
         * Builds a map from some builtin objects to their canonical path
         */
        function makeBuiltinsMap(){
            var map = new Map/*<Object, Path>*/();

            var roots = {
                Object: Object,
                Function: Function,
                Array: Array,
                RegExp: RegExp,
                Date: Date,
                'Object.prototype': Object.prototype,
                'Function.prototype': Function.prototype,
                'Array.prototype': Array.prototype,
                'RegExp.prototype': RegExp,
                'Date.prototype': Date.prototype
            };

            map.set(Object, 'Object');
            map.set(Function, 'Function');
            map.set(Array, 'Array');
            map.set(RegExp, 'RegExp');
            map.set(Date, 'Date');
            for(var prefix in roots){
                var root = roots[prefix];
                Object.getOwnPropertyNames(root).forEach(
                    function(propertyName){
                        var propertyValue = root[propertyName];
                        if(typeof propertyValue === 'function' || typeof propertyValue === 'object'){
                            var path = prefix + '.' + propertyName;
                            map.set(propertyValue, path);
                        }
                    }
                );
            }
            return map;
        }

        function describeNonPrimitive(val){
            if(allocationSites.has(val)){
                return "allocation:" + getFullLocation(allocationSites.get(val));
            }

            if(builtins.has(val)){
                return "builtin:" + builtins.get(val);
            }

            return "other";
        }

		function p(val) {
			var t = typeof val;
			if (t == "undefined" || t == "boolean")
				return val;
			if (t == "number") {
				if (val == 0 || val == 1)
					return val;
				if (isNaN(val))
					return "NUM_NAN";
				if (!isFinite(val))
					return "NUM_INF";
				if (val >= 0 && val < 4294967295 && (val % 1) == 0)
					return "NUM_UINT";
				return "NUM_OTHER";
			}
			if (t == "string") {
				if(!isNaN(val) && val >= 0 && val < 4294967295 && (val % 1) == 0){
					return "STR_UINT";
				}
				if(!isNaN(val)){
					return "STR_OTHERNUM";
				}
				if(val.match("^[_$a-zA-Z\xA0-\uFFFF][_$a-zA-Z0-9\xA0-\uFFFF]*$") && !isReservedName(val)){ 
					//identifiers - Not precise - See http://stackoverflow.com/questions/2008279/validate-a-javascript-function-name/2008444#2008444
					return "STR_IDENTIFIER";
				}
				if(val.match("^[_$a-zA-Z0-9\xA0-\uFFFF]*$")){
					//identifierParts - Not precise - See http://stackoverflow.com/questions/2008279/validate-a-javascript-function-name/2008444#2008444
					return "STR_IDENTIFIERPARTS";
				}
				if(val.match("^((?![_$a-zA-Z0-9\xA0-\uFFFF]).)+[_$a-zA-Z0-9\xA0-\uFFFF]*$")){
					//Prefix - Not precise - See http://stackoverflow.com/questions/2008279/validate-a-javascript-function-name/2008444#2008444
					return "STR_PREFIX"
				}
				//if()
					//return "STR_JSON" //TODO STR_JSON abstraction

				return "STR_OTHER"

			}
			if (val === null)
				return "null";
			if (t == "function") {
				return "Function[" + describeNonPrimitive(val) + "]"
			}
			if (t == "object") {
				return "Object["+ describeNonPrimitive(val) +"]";
			}
			return "???";
		}

		function pa(args) {
			var s = "[";
			for (var i = 0; i < args.length; i++)
				s += ((i === 0)? "": ":") + p(args[i]);
			return s + "]";
		}

        function getFullLocation(iid){
            return sandbox.iidToLocation(sandbox.getGlobalIID(iid));
        }

		function log(iid, m) {
			console.log(getFullLocation(iid) + ":" + m);
		}

		this.read = function(iid, name, val, isGlobal, isScriptLocal) {
			log(iid, "read-variable:" + s(name) + ":" + p(val));
		};

		this.write = function(iid, name, val, lhs, isGlobal, isScriptLocal) {
			log(iid, "write-variable:" + s(name) + ":" + p(val));
		};

		this.getField = function(iid, base, offset, val, isComputed, isOpAssign, isMethodCall) {
			log(iid, "read-property:" + s(offset) + ":" + p(val));
		};

		this.putField = function(iid, base, offset, val, isComputed, isOpAssign) {
			log(iid, "write-property:" + s(offset) + ":" + p(val));
		};

		this.invokeFunPre = function(iid, f, base, args, isConstructor, isMethod) {
			log(iid, "call:" + s(f) + ":" + p(base) + ":" + pa(args));
		};

        this.invokeFun = function (iid, f, base, args, result, isConstructor, isMethod, functionIid) {
            if(isConstructor && !allocationSites.has(result) /* the constructor could create and return its own result... */){
                registerAllocation(iid, result);
            }
        };

        this.functionEnter = function(iid, f, dis, args) {
            log(iid, "function-entry:" + p(dis) + pa(args));
		};

		this.functionExit = function(iid, returnVal, wrappedExceptionVal) {
			log(iid, "function-exit:" + p(returnVal) + ":" + p(wrappedExceptionVal));
		};

        this.literal = function (iid, val, hasGetterSetter) {
            registerAllocation(iid, val);
        };

        function registerAllocation(iid, val){
            if (typeof val === 'function' || typeof val === 'object') {
                allocationSites.set(val, iid);
            }
        }

		function s(val) {
			if (typeof val == "string") {
				return JSON.stringify(val);
			} else {
				return p(val);
			}
		}

		/**
		 * Checks whether the given string is a reserved name.
		 */
		function isReservedName(s) {
			if (s = "")
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
