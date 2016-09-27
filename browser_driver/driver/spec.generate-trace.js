var mkdirp = require('mkdirp');
var fs = require('fs');
var path = require('path');
var util = require('util');

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
    var dest = path.join(browser.params.outdir, dest);

    if (typeof content === 'function') {
        var writer = fs.createWriteStream(dest);
        var data;
        while ((data = content()) !== null) {
            writer.write(data, 'utf8');
        }
        writer.end();
        deferred.fulfill();
    } else {
        fs.writeFile(dest, content, function(err) {
            if(err) {
                console.log(err);
                deferred.reject();
            } else {
                console.log("The file " + dest + " was saved!");
                deferred.fulfill();
            }
        });
    }

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
        var terminatedInTime = true;

        var waitStart = new Date().getTime();
        var lastNotification = waitStart;
        browser.wait(function () {
            var time = new Date().getTime();
            if (time - waitStart > WAIT_TIMEOUT * 1000) {
                console.log("Waited for " + WAIT_TIMEOUT + " seconds, terminating anyway");
                terminatedInTime = false;
                return true;
            } else if (time - lastNotification > NOTIFICATION_STEP * 1000) {
                console.log("Still waiting for Jalangi...");
                lastNotification = time;
            }
            return browser.executeScript("return !!(window.J$ && window.J$.analysis);");
        }, 1200000);

        browser.executeScript("void(0);").then(function () {
            if (!terminatedInTime) {
                return;
            }

            console.log('Done waiting for Jalangi; persisting data...');
            return browser.executeScript('return window.logEntries.length;')
                .then(function (numEntries) {
                    console.log(util.format('Fetching %s log entries...', numEntries));

                    function retrieveMessagesFromTo(from, to) {
                        console.log(util.format('Fetching log entries #%s to #%s', from+1, to));
                        return browser
                            .executeScript(util.format("return J$.analysis.getOutputAsString(%s, %s);", from, to))
                            .then(JSON.parse);
                    }

                    var promise = retrieveMessagesFromTo(0, Math.min(10000, numEntries));

                    var messagesRetrieved = 10000;
                    while (numEntries > messagesRetrieved) {
                        (function (from, to) {
                            promise = promise.then(function (entries) {
                                return retrieveMessagesFromTo(from, to).then(function (other) {
                                    return entries.concat(other);
                                });
                            });
                        })(messagesRetrieved, Math.min(messagesRetrieved+10000, numEntries));
                        messagesRetrieved += 10000;
                    }

                    return promise;
                }).then(function (entries) {
                    // A function that allows the `save` function to retrieve the data chunk-by-chunk.
                    // (For some applications, the combined string is too big to be concatenated, so
                    // we must stream it to the file system.)
                    var i = 0, linebreak = false;
                    function getData() {
                        if (linebreak) {
                            linebreak = false;
                            return '\n';
                        }
                        if (i < entries.length) {
                            if (i < entries.length - 1) {
                                linebreak = true;
                            }
                            return JSON.stringify(entries[i++]);
                        }
                        return null;
                    }

                    save("NEW_LOG_FILE.log", getData).then(function () {
                        getBrowserLog().then(function (blog) {
                            save("log.trace-program-generation.txt", blog).then(function () {
                                var filtered = blog.replace(/Uncaught ExpectedTopLevelError/g, '');
                                expect(filtered).not.toContain('Assertion failed');
                                expect(filtered).not.toContain('Uncaught');
                                done();
                            });
                        });
                    });
                });
        });

    }, MAX_SAFE_TIMEOUT);
});
