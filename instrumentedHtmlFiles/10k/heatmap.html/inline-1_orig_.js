
    Canvas = function(element, options) {
        this.init(element, options);
    }
    Canvas.prototype = {
        init: function(options) {
            this.options = options;
            this._createCanvasFromElement();
        },
        getContext: function() {
            return this.cx;
        },
        getHTMLElement: function() {
            return this.c;
        },
        w: function() {
            return this.c.width;
        },
        h: function() {
            return this.c.height;
        },
        clear: function(color) {
            var c = this.cx;
            c.globalCompositeOperation = "source-over";
            c.fillStyle = color || "#FFF";
            c.fillRect(0, 0, this.w(), this.h());
        },
        drawImage: function(image, x, y) {
            this.cx.drawImage(image, x, y);
        },
        putImageData: function(data, x, y) {
            this.cx.putImageData(data, x, y);
        },
        getImageData: function(x, y, width, height) {
            return this.cx.getImageData(x, y, width, height);
        },

        _createCanvasFromElement: function() {
            var canvas = document.createElement('canvas'),
                    element = this.options.element;
            canvas.className = 'heatmap';
            if (element) {
                canvas.width = element.offsetWidth;
                canvas.height = element.offsetHeight;
            } else {
                canvas.width = this.options.width;
                canvas.height = this.options.height;
            }
            this.c = canvas;
            this.cx = canvas.getContext('2d');
        }
    }


