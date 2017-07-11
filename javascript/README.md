# Value logger analysis (jalangilogger) 

A [jalangi2](https://github.com/Samsung/jalangi2) analysis for recording values of expressions.

When run on Node (or Nashorn), the the recorded values are written to a file on disk.
Whenrun in a browser, the recorded values are sent to a local server.

Meaningful use of this analysis requires a complex setup that is implemented with Java in the main [jalangi logger project](https://github.com/cs-au-dk/jalangilogger). That project also contains the source code for this analysis.

# NPM guide 

This module is registered on npm as [jalangilogger](https://www.npmjs.com/package/jalangilogger).
Changes should be pushed with `npm publish`.
(an alternative would be to embed an npm-install script in the jar of the java-project).
