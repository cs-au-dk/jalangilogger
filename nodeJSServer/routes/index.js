var express = require('express');
var router = express.Router();

var entries = [];

var gracefulShutdown = function (server) {
    server.close(function () {
        process.exit(0);
    });

    setTimeout(function () {
        process.exit();
    }, 10 * 1000);
}

router.post('/sendEntries', function (req, res, next) {
    console.log("Received entries")
    res.status(204); //No Content response
    res.setHeader('Access-Control-Allow-Origin', 'null');
    var entriesString = req.body["entries"];
    try {
        JSON.parse(entriesString).forEach(entry => entries.push(entry));
    } catch (e) {
        console.error(entriesString);
        throw e;
    }
    res.end();
});

router.post('/done', function (req, res) {
    res.setHeader('Access-Control-Allow-Origin', 'null');
    res.status(204) //No Content response

    var fs = require('fs');

    var file = fs.createWriteStream("logfile");
    file.on('error', function (err) {
        console.error(err)
    });
    file.on('open', function (fd) {
        entries.forEach(function (v) {
            file.write(JSON.stringify(v) + '\n')
        });
        file.end;
        entries = [];
        //kill server after printToFile!!!
        req.socket.server.close();
        gracefulShutdown(req.socket.server);
    });

    res.end();
})

module.exports = router;
