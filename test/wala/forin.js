function testForIn( x ) {
  var z = "";
  for(var y in x) {
    z += (x[y])();
  }
  return z;
}

var q = testForIn({
  bar: function testForIn2() { return "whatever"; },
	afoo: function testForIn1() { return 7; }
});

TAJS_assert(q, 'isMaybeStrUInt');
TAJS_assert(q, 'isMaybeStrIdentifierParts');

