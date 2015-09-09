J$.iids = {"8":[5,7,5,24],"9":[2,1,2,7],"16":[5,7,5,24],"17":[2,25,2,31],"25":[3,12,3,20],"33":[3,35,3,43],"41":[3,12,3,44],"43":[3,12,3,34],"49":[3,12,3,44],"57":[3,12,3,44],"65":[5,7,5,8],"73":[5,12,5,13],"81":[5,12,5,24],"89":[6,4,6,18],"97":[6,19,6,20],"105":[6,4,6,21],"113":[6,4,6,22],"121":[2,33,8,2],"129":[2,33,8,2],"137":[2,33,8,2],"145":[2,33,8,2],"153":[8,4,8,9],"161":[2,1,8,10],"163":[2,1,2,24],"169":[2,1,8,11],"177":[1,1,9,1],"185":[5,3,7,4],"193":[2,33,8,2],"201":[2,33,8,2],"209":[1,1,9,1],"217":[1,1,9,1],"nBranches":6,"originalCodeFileName":"inline-0_orig_.js","instrumentedCodeFileName":"inline-0.js","code":"\nwindow.addEventListener(\"load\", function() {\n   var c = document.createElement(\"canvas\");\n  //  TAJS_dumpValue(c); // If you outcomment this, the type of c becomes HTMLCanvasElement.prototype.getContext\n  if (c && c.getContext) {\n   TAJS_dumpValue(c); // The type is HTMLCanvasElement, unless the line above is outcommented??\n  }\n}, false);\n"};
jalangiLabel1:
    while (true) {
        try {
            J$.Se(177, 'inline-0.js', 'inline-0_orig_.js');
            J$.X1(169, J$.M(161, J$.I(typeof window === 'undefined' ? window = J$.R(9, 'window', undefined, true, true) : window = J$.R(9, 'window', window, true, true)), 'addEventListener', false, false)(J$.T(17, 'load', 21, false), J$.T(145, function () {
                jalangiLabel0:
                    while (true) {
                        try {
                            J$.Fe(121, arguments.callee, this, arguments);
                            arguments = J$.N(129, 'arguments', arguments, true, false, false);
                            J$.N(137, 'c', c, false, false, false);
                            var c = J$.X1(57, J$.W(49, 'c', J$.M(41, J$.I(typeof document === 'undefined' ? document = J$.R(25, 'document', undefined, true, true) : document = J$.R(25, 'document', document, true, true)), 'createElement', false, false)(J$.T(33, 'canvas', 21, false)), c, false, false, true));
                            if (J$.X1(185, J$.C(16, J$.C(8, J$.R(65, 'c', c, false, false)) ? J$.G(81, J$.R(73, 'c', c, false, false), 'getContext', false) : J$._()))) {
                                J$.X1(113, J$.F(105, J$.I(typeof TAJS_dumpValue === 'undefined' ? TAJS_dumpValue = J$.R(89, 'TAJS_dumpValue', undefined, true, true) : TAJS_dumpValue = J$.R(89, 'TAJS_dumpValue', TAJS_dumpValue, true, true)), false)(J$.R(97, 'c', c, false, false)));
                            }
                        } catch (J$e) {
                            J$.Ex(193, J$e);
                        } finally {
                            if (J$.Fr(201))
                                continue jalangiLabel0;
                            else
                                return J$.Ra();
                        }
                    }
            }, 12, false, 121), J$.T(153, false, 23, false)));
        } catch (J$e) {
            J$.Ex(209, J$e);
        } finally {
            if (J$.Sr(217)) {
                J$.L();
                continue jalangiLabel1;
            } else {
                J$.L();
                break jalangiLabel1;
            }
        }
    }
// JALANGI DO NOT INSTRUMENT
