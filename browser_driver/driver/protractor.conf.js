var argv = require('yargs')
    .usage('Usage: protractor protractor.conf.js [--proxy] --url <url>')
    .option('compareUrl', { demand: false, type: 'string' })
    .option('mode', { default: 'generate-trace', demand: false, choices: ['detect-errors', 'generate-trace'] })
    .option('outdir', { default: 'out', demand: false, type: 'string' })
    .option('spec', { demand: false, type: 'string' })
    .option('url', { demand: true, type: 'string' })
    .option('proxy', { default: false, demand: false, type: 'boolean' })
    .argv;

//console.log("Running Protractor, with args:")
//console.log(argv)

var chromeArgs = ['--no-sandbox', '--show-fps-counter=true', '-kiosk'];
if (argv.proxy) {
    chromeArgs.push('--proxy-server=127.0.0.1:8081', '--proxy-bypass-list=\'\'');
}

exports.config = {
    directConnect: true,
    framework: 'jasmine2',
    specs: ['spec.' + argv.mode + '.js'],
    capabilities: {
        browserName: 'chrome',
        chromeOptions: {
            args: chromeArgs
        },
        loggingPrefs: {
            'driver': 'INFO',
            'browser': 'INFO'
        }
    },
    javascriptEnabled: true,
    handlesAlerts: true,
    loggingPrefs: { browser: 'ALL', driver: 'ALL' },
    params: argv
};
