var mkdirp = require('mkdirp');
var fs = require('fs');
var path = require('path');

jasmine.DEFAULT_TIMEOUT_INTERVAL = 2000;
var MAX_SAFE_TIMEOUT = Math.pow(2, 31) - 1;
var NOTIFICATION_STEP = 10;
var WAIT_TIMEOUT = 60;

mkdirp.sync(browser.params.outdir);

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


describe('Trace generation', function () {
    //browser.manage().timeouts().pageLoadTimeout(10000);
    it('open web page', function (done) {
        console.log("Opening " + browser.params.url);
        // console.log(browser.params);

        browser.driver.get(browser.params.url);
        browser.ignoreSynchronization = true;

        console.log("Waiting for Jalangi...");

        // Wait to finish... not working at the moment
        var waitStart = new Date().getTime();
        var lastNotification = waitStart;
        browser.wait(function () {
            var time = new Date().getTime();
            if (time - waitStart > WAIT_TIMEOUT * 1000) {
                console.log("Waited for " + WAIT_TIMEOUT + " seconds, terminating anyway");
                return true;
            } else if (time - lastNotification > NOTIFICATION_STEP * 1000) {
                console.log("Still waiting for Jalangi...");
                lastNotification = time;
            }
            return browser.executeScript("return !!(window.J$ && window.J$.analysis);");
        }, 1200000);

        browser.executeScript("void(0);").then(function () {
            console.log('Done waiting for Jalangi; persisting data...');
        });


        browser.executeScript("return JSON.stringify({ \"entries\": window.logEntries });").then(function (content) {
            var res = JSON.parse(content);
            save("NEW_LOG_FILE.log", res.entries.map((x) => JSON.stringify(x)).join("\n")).then(function () {
                getBrowserLog().then(function (blog) {
                    save("log.trace-program-generation.txt", blog).then(function () {
                        var filtered = blog.replace(/Uncaught ExpectedTopLevelError/g, '');
                        expect(filtered).not.toContain('Assertion failed');
                        expect(filtered).not.toContain('Uncaught');
                        done();
                    });
                });
            })
        });

    }, MAX_SAFE_TIMEOUT);
});
