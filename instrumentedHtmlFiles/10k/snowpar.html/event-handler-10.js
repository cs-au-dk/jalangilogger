J$.iids = {"9":[1,1,1,7],"17":[1,16,1,20],"25":[1,16,1,26],"33":[1,1,1,26],"41":[1,1,1,27],"49":[1,28,1,34],"57":[1,44,1,48],"65":[1,44,1,54],"73":[1,28,1,54],"81":[1,28,1,55],"89":[1,56,1,67],"97":[1,56,1,69],"105":[1,56,1,70],"113":[1,71,1,83],"121":[1,84,1,92],"129":[1,71,1,93],"137":[1,71,1,94],"145":[1,1,1,94],"153":[1,1,1,94],"161":[1,1,1,94],"nBranches":2,"originalCodeFileName":"event-handler-10_orig_.js","instrumentedCodeFileName":"event-handler-10.js","code":"canvas.width = this.value; canvas.height = this.value; initSnowPos(); fixPositions('canvas');"};
jalangiLabel10:
    while (true) {
        try {
            J$.Se(145, 'event-handler-10.js', 'event-handler-10_orig_.js');
            J$.X1(41, J$.P(33, J$.I(typeof canvas === 'undefined' ? canvas = J$.R(9, 'canvas', undefined, true, true) : canvas = J$.R(9, 'canvas', canvas, true, true)), 'width', J$.G(25, J$.R(17, 'this', this, false, false), 'value', false), false));
            J$.X1(81, J$.P(73, J$.I(typeof canvas === 'undefined' ? canvas = J$.R(49, 'canvas', undefined, true, true) : canvas = J$.R(49, 'canvas', canvas, true, true)), 'height', J$.G(65, J$.R(57, 'this', this, false, false), 'value', false), false));
            J$.X1(105, J$.F(97, J$.I(typeof initSnowPos === 'undefined' ? initSnowPos = J$.R(89, 'initSnowPos', undefined, true, true) : initSnowPos = J$.R(89, 'initSnowPos', initSnowPos, true, true)), false)());
            J$.X1(137, J$.F(129, J$.I(typeof fixPositions === 'undefined' ? fixPositions = J$.R(113, 'fixPositions', undefined, true, true) : fixPositions = J$.R(113, 'fixPositions', fixPositions, true, true)), false)(J$.T(121, 'canvas', 21, false)));
        } catch (J$e) {
            J$.Ex(153, J$e);
        } finally {
            if (J$.Sr(161)) {
                J$.L();
                continue jalangiLabel10;
            } else {
                J$.L();
                break jalangiLabel10;
            }
        }
    }
// JALANGI DO NOT INSTRUMENT
