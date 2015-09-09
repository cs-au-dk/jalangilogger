
    (function() {
        function loremIpsum() {
            var text = ['<h1>Lorem ipsum dolor sit amet</h1>',
                '<h2>Consectetur adipiscing elit</h2>',
                '<p>Eset eiusmod tempor incidunt et labore et dolore magna aliquam. Ut enim ad minim veniam,',
                ' quis nostrud exerc. Irure dolor in reprehend incididunt ut labore et dolore magna aliqua.',
                ' Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.',
                ' Duis aute irure dolor in reprehenderit in voluptate velit esse molestaie cillum.',
                ' Tia non ob ea soluad incommod quae egen ium improb fugiend.</p>'];
            return text.join('');
        }

        function $(id) {
            return document.getElementById(id)
        }

        var c = $('container'), o = $('opacity'), s = $('scheme');
        for (var i = 0; i < 10; i++) {
            c.innerHTML += loremIpsum();
        }

        function idle() {
            timer = setTimeout(idle, 100);
            h.add(lx, ly);
        }

        function add(event) {
            if (event.targetTouches) {
                event.preventDefault();
                var x = event.targetTouches[0].pageX - c.offsetLeft - c.parentNode.offsetLeft,
                        y = event.targetTouches[0].pageY - c.offsetTop - c.parentNode.offsetTop;
                h.add(x, y);
            }
            else {
                var x = parseInt(event.layerX), y = parseInt(event.layerY);
                if (Math.abs(x - lx) > 3 || Math.abs(y - ly) > 3) {
                    lx = x;
                    ly = y;
                    h.add(x, y);
                }
            }
        }

        var canvas = new Canvas({element: c}),
                h = new Heatmap(canvas),
                e = canvas.getHTMLElement(),
                lx = 0,
                ly = 0,
                timer;

        c.appendChild(e);

        if ('createTouch' in document) { // Thanks Thomas!
            e.addEventListener("touchstart", add, false);
        } else {
            e.addEventListener("mousemove", function (event) {
                add(event);
                clearTimeout(timer);
                timer = setTimeout(idle, 100);
            }, false);
        }
        e.addEventListener('mouseout', function() {
            clearTimeout(timer)
        }, false);

        o.addEventListener('change', function() {
            e.style.opacity = (100 - (o.selectedIndex * 25)) / 100;
        }, false);
        s.addEventListener('change', function() {
            h.setScheme(s.selectedIndex)
        }, false);
    })();
