(function (sandbox) {

    function MyAnalysis () {

	var info = {};

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
	    if (t == "string") { // TODO: abstract string values?
		return JSON.stringify(val);
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

    }
    sandbox.analysis = new MyAnalysis();
})(J$);
