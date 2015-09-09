
        function createTerrain() {
            //Set up global stuff
            var initialHeight = parseFloat(document.getElementById("height").value);
            theCoords = [initialHeight, initialHeight];
            extremity = parseFloat(document.getElementById("extremity").value);
            detail = parseInt(document.getElementById("detail").value);
            xsize = parseInt(document.getElementById("xsize").value);
            ysize = parseInt(document.getElementById("ysize").value);
            var mountain = document.getElementById("mv").selectedIndex;
            if (mountain == 1) {
                displacement = 1.0;
            }
            else {
                displacement = -1.0;
            }
            IntervalCounter = 0;

            canvas = document.getElementById('landscape');
            canvas.height = ysize;
            canvas.width = xsize;
            // use getContext to use the canvas for drawing
            ctx = canvas.getContext('2d');
            lineargradient = ctx.createLinearGradient(xsize / 2, ysize / 2, xsize / 2, ysize);
            color1 = "rgb(" + Math.floor(Math.random() * 255) + "," + Math.floor(Math.random() * 255) + "," + Math.floor(Math.random() * 255) + ")"
            color2 = "rgb(" + Math.floor(Math.random() * 255) + "," + Math.floor(Math.random() * 255) + "," + Math.floor(Math.random() * 255) + ")"
            lineargradient.addColorStop(0, color1);
            lineargradient.addColorStop(1, color2);

            animInterval = setInterval("animatedDraw()", 100);
        }

        function animatedDraw() {
            if (IntervalCounter < detail) {
                theCoords = fractalize(theCoords, displacement *= extremity);
                drawLandscape(theCoords);
                IntervalCounter++;
            }
            else {
                clearInterval(animInterval);
            }
        }

        function fractalize(theCoords, displacement) {
            //split the line segments up
            if (theCoords.Count < 2) {
                alert(">= 2 coordinates required");
            }
            var newCoords = new Array();
            var theLength = theCoords.length - 1;
            var i;
            for (i = 0; i < theLength; i++) {

                var mrRandom = Math.random();
                var displacedCoord = (theCoords[i + 1] - theCoords[i]) / 2.0;
                var newDisplacement = mrRandom * displacement;
                var nextCoord = theCoords[i] + displacedCoord + newDisplacement;

                newCoords.push(theCoords[i]);
                newCoords.push(nextCoord);
            }
            newCoords.push(theCoords[theCoords.length - 1]);
            return newCoords;
        }

        function drawLandscape(theCoords) {

            ctx.clearRect(0, 0, xsize, ysize);
            ctx.fillStyle = lineargradient;
            ctx.beginPath();
            ctx.moveTo(0, ysize);

            var j;
            for (j = 0; j < theCoords.length; j++) {
                xCoord = (xsize / (theCoords.length - 1))
                xCoord = xCoord * j;
                yCoord = ysize - (theCoords[j] * ysize);
                ctx.lineTo(xCoord, yCoord);
            }
            ctx.lineTo(xsize, ysize);
            ctx.fill();
        }


    