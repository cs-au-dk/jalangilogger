
    Heatmap = function(c) {
        var buffer = new Canvas({width: c.w(), height: c.h()}),
                dot, scheme = [], current = 0;

        preload("images/dot2.png", function(data) {
            dot = data;
        });
        preload("images/classic.png", function(data) {
            scheme[0] = data;
        });
        preload("images/fire.png", function(data) {
            scheme[1] = data;
        });

        buffer.clear();

        function add(x, y) {
            if (!dot)    return;

            var dotPix = dot.data,
                    x = x - dot.width / 2,
                    y = y - dot.height / 2,
                    data = buffer.getImageData(x, y, dot.width, dot.height);
            dataPix = data.data

            for (var i = 0, n = dataPix.length; i < n; i += 4) {
                var s = dotPix[i] / 255;
                s = (1 - (1 - s) / 3);
                dataPix[i] *= s;
                dataPix[i + 1] *= s;
                dataPix[i + 2] *= s;
            }
            buffer.putImageData(data, x, y);
            redraw(x, y, dot.width, dot.height);
        }

        function preload(imageName, callback) {
            var i = new Image();
            i.src = imageName;
            i.onload = function(event) {
                var b = new Canvas({width: i.width, height: i.height});
                b.drawImage(i, 0, 0);
                callback(b.getImageData(0, 0, i.width, i.height));
            };
        }

        function redraw(x, y, width, height) {
            if (!scheme[current])    return;

            var data = buffer.getImageData(x, y, width, height),
                    pix = data.data,
                    s = scheme[current].data;
            for (var i = 0, n = pix.length; i < n; i += 4) {
                var value = pix[i], heatValue = value << 2;
                pix[i  ] = s[heatValue];
                pix[i + 1] = s[heatValue + 1];
                pix[i + 2] = s[heatValue + 2];
                pix[i + 3] = value < 200 ? 255 : 255 - value;
            }
            c.putImageData(data, x, y);
        }

        function setScheme(i) {
            current = i;
            redraw(0, 0, c.w(), c.h());
        }

        return {add: add, setScheme:setScheme}
    };

