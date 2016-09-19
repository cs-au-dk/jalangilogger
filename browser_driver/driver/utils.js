var fs = require('fs');
var path = require('path');

function getPosition(str, pattern, occurrence) {
    return str.split(pattern, occurrence).join(pattern).length;
}

function getBrowserLog() {
    return browser.manage().logs().get('browser').then(function(blog) {
        return blog.map((x) => x.message.substring(getPosition(x.message, " ", 2))).join('\n');
    });
}

function save(dest, content) {
    var deferred = protractor.promise.defer();
    fs.writeFile(path.join(browser.params.outdir, dest), content, function(err) {
        if(err) {
            console.log(err);
            deferred.reject();
        } else {
            console.log("The file " + dest + " was saved!");
            deferred.fulfill();
        }
    });
    return deferred.promise;
}

exports.getBrowserLog = getBrowserLog;
exports.save = save;