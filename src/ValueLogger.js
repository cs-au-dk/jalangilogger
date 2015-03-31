
(function (sandbox) {

	function MyAnalysis () {

		var info = {};
		var reservedWords = {}
		
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
				var intVal = parseInt(val);
				if(!isNaN(intVal) && intVal >= 0 && intVal < 4294967295 && (intVal % 1) == 0){
					return "STR_UINT";
				}
				if(!isNaN(intVal) || val == "Infinity" || val == "-Infinity" || val == "NaN"){
					return "STR_OTHERNUM"
				}
				if(val.match("^[_$a-zA-Z\xA0-\uFFFF][_$a-zA-Z0-9\xA0-\uFFFF]*$)" && !isReservedName(val))){ 
					//identifiers - Not precise - See http://stackoverflow.com/questions/2008279/validate-a-javascript-function-name/2008444#2008444
					return "STR_IDENTIFIER";
				}
				if(val.match("^[_$a-zA-Z0-9\xA0-\uFFFF]*$)")){ 
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
			if (t == "function") { // TODO: track source locations of object and function values?
				return "Function[" + p(val.name) + "]"
			}
			if (t == "object") {
				return "Object["+val.constructor.name+"]";
			}
			return "???";
		}

		function pa(args) {
			var s = "[";
			for (var i = 0; i < args.length; i++)
				s += ":" + p(args[i]);
			return s + "]";
		}

		function log(iid, m) {
			console.log(sandbox.iidToLocation(sandbox.getGlobalIID(iid)) + ":" + m);
		}

		this.read = function(iid, name, val, isGlobal, isScriptLocal) {
			log(iid, "read-variable:" + p(name) + ":" + p(val));
		};

		this.write = function(iid, name, val, lhs, isGlobal, isScriptLocal) {
			log(iid, "write-variable:" + p(name) + ":" + p(val));
		};

		this.getField = function(iid, base, offset, val, isComputed, isOpAssign, isMethodCall) {
			log(iid, "read-property:" + p(offset) + ":" + p(val));
		};

		this.putField = function(iid, base, offset, val, isComputed, isOpAssign) {
			log(iid, "write-property:" + p(offset) + ":" + p(val));
		};

		this.invokeFunPre = function(iid, f, base, args, isConstructor, isMethod) {
			log(iid, "call:" + p(f) + ":" + p(base) + pa(args));
		};

		this.functionEnter = function(iid, f, dis, args) {
			log(iid, "function-entry:" + p(dis) + pa(args));
		};

		this.functionExit = function(iid, returnVal, wrappedExceptionVal) {
			log(iid, "function-exit:" + p(returnVal) + ":" + p(wrappedExceptionVal));
		};
		
		/**
		 * Checks whether the given string is a reserved name.
		 */
		function isReservedName(s) {
			if (s.isEmpty())
				return false;
			switch (s.charAt(0)) {
			case 'a':
				return s.equals("abstract"); 
			case 'b':
				return s.equals("boolean") || s.equals("break") || s.equals("byte");
			case 'c':
				return s.equals("case") || s.equals("catch") || s.equals("char") || s.equals("class") 
				|| s.equals("const") || s.equals("continue");
			case 'd':
				return s.equals("debugger") || s.equals("default") || s.equals("delete") || s.equals("do") 
				|| s.equals("double");
			case 'e':
				return s.equals("else") || s.equals("enum") || s.equals("export") || s.equals("extends"); 
			case 'f':
				return s.equals("false") || s.equals("final") || s.equals("finally") || s.equals("float") 
				|| s.equals("for") || s.equals("function");
			case 'g':
				return s.equals("goto");
			case 'i':
				return s.equals("if") || s.equals("implements") || s.equals("import") || s.equals("in") 
				|| s.equals("instanceof") || s.equals("int") || s.equals("interface");
			case 'l':
				return s.equals("long");
			case 'n':
				return s.equals("native") || s.equals("new") || s.equals("null");
			case 'p':
				return s.equals("package") || s.equals("private") || s.equals("protected") || s.equals("public");
			case 'r':
				return s.equals("return");
			case 's':
				return s.equals("short") || s.equals("static") || s.equals("super") || s.equals("switch") 
				|| s.equals("synchronized");
			case 't':
				return s.equals("this") || s.equals("throw") || s.equals("throws") || s.equals("transient") 
				|| s.equals("true") || s.equals("try") || s.equals("typeof");
			case 'v':
				return s.equals("var") || s.equals("void") || s.equals("volatile");
			case 'w':
				return s.equals("while") || s.equals("with");
			default:
				return false;
			}
		}

	}
	sandbox.analysis = new MyAnalysis();
})(J$);
