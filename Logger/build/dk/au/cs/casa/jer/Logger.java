package dk.au.cs.casa.jer;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Produces a log file
 */
public class Logger {

    private final Path root;

    private final Path rootRelativeTestDir;

    private final Path rootRelativeMain;

    private final Path instrumentationDir;

    private final Path temp;

    private final Path analysis;

    private final Path node;

    private final Path jalangilogger;

    /**
     * Produces a log file for the run of a single main file
     */
    public Logger(Path root, Path rootRelativeMain, Path node, Path jalangilogger) {
        this(isolateInNewRoot(root, rootRelativeMain), rootRelativeMain.getParent(), rootRelativeMain, node, jalangilogger);
    }

    /**
     * Produces a log file for the run of a main file in a directory
     */
    public Logger(Path root, Path rootRelativeTestDir, Path rootRelativeMain, Path node, Path jalangilogger) {
        if(rootRelativeTestDir.isAbsolute()){
            throw new IllegalArgumentException("rootRelativeTestDir must be relative");
        }
        if(rootRelativeMain.isAbsolute()){
            throw new IllegalArgumentException("rootRelativeMain must be relative");
        }
        this.root = root;
        this.rootRelativeTestDir = rootRelativeTestDir;
        this.rootRelativeMain = rootRelativeMain;
        this.node = node;
        this.jalangilogger = jalangilogger;
        try {
            this.temp = createTempDirectory();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.instrumentationDir = temp.resolve("instrumentation");
        this.analysis = jalangilogger.resolve("logger/src/ValueLogger.js").toAbsolutePath();
    }

    private static Path createTempDirectory() throws IOException {
        return Files.createTempDirectory(Logger.class.getCanonicalName());
    }

    /**
     * Isolates a single file in a new empty directory. Relative paths behave as before.
     *
     * @return the new root directory
     */
    private static Path isolateInNewRoot(Path root, Path rootRelativeMain) {
        try {
            Path newRoot = createTempDirectory();
            Path isolated = newRoot.resolve(rootRelativeMain);
            Files.createDirectories(isolated.getParent());
            Files.copy(root.resolve(rootRelativeMain), isolated);
            return newRoot;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path filterLogFile(Path inputLog) throws IOException {
        List<String> lines = Files.readAllLines(inputLog).stream()
                .map(String::trim)
                .filter(l -> !l.isEmpty())
                .map(l -> l.startsWith(",") ? l.substring(1) : l)
                .distinct()
                .collect(Collectors.toList());
        Path filtered = inputLog.getParent().resolve(inputLog.getFileName().toString() + ".filtered");
        Files.write(filtered, lines);
        return filtered;
    }

    private Path addMeta(Path log, String exitStatus) throws IOException {
        String hash = HashUtil.shaDirOrFile(root.resolve(rootRelativeTestDir));
        long time = System.currentTimeMillis();
        String root = rootRelativeTestDir.toString();
        String meta = String.format("{'sha':'%s', 'time':'%d', 'root':'%s', 'result':'%s'}".replaceAll("'", "\""), hash, time, root, exitStatus);
        List<String> lines = Files.readAllLines(log);
        lines.add(0, meta);
        Path outputLog = log.getParent().resolve(log.getFileName().toString() + ".metaed");
        Files.write(outputLog, lines);
        return outputLog;
    }

    private Process exec(Path pwd, String... cmd) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (pwd != null) {
            pb.directory(pwd.toFile());
        }
        //pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        System.out.printf("Starting (at %s): %s", pwd, String.join(" ", Arrays.asList(cmd)));
        Process p = pb.start();
        return p;
    }

    public Path log() throws IOException {
        String mainName = rootRelativeMain.toString();
        if (mainName.endsWith(".js")) {
            return new JSLogger().log();
        } else if (mainName.endsWith(".html")) {
            return new HTMLLogger().log();
        }
        throw new IllegalArgumentException("Unsupported extension of main-file: " + mainName);
    }

    private void instrument() throws IOException {
        Path instrument_js = jalangilogger.resolve("node_modules/jalangi2").resolve("src/js/commands/instrument.js").toAbsolutePath();
        String script = instrument_js.toString();
        String out = instrumentationDir.resolve(rootRelativeTestDir.getParent()).toAbsolutePath().toString();
        String in = rootRelativeTestDir.toString();
        String[] cmd = new String[]{node.toString(), script, "--inlineIID", "--inlineSource", "-i", "--inlineJalangi", "--analysis", analysis.toString(), "--outputDir", out, in};
        Process exec = exec(root, cmd);
        try {
            exec.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (exec.exitValue() != 0) {
            throw new RuntimeException("Instrumentation failed");
        }
    }

    private Path postProcessLog(Path logFile) throws IOException {
        Path filtered = filterLogFile(logFile);
        Path transformed = new LogFileTransformer(root, instrumentationDir, rootRelativeMain).transform(filtered);
        return transformed;
    }

    private class HTMLLogger {

        private final Path serverDir;

        public HTMLLogger() {
            this.serverDir = temp.resolve("server");
        }

        private void stopServer(Process server) {
            server.destroy();
        }

        private void openBrowser() throws IOException {
            Desktop.getDesktop().browse(instrumentationDir.resolve(rootRelativeMain).toUri());
        }

        private Process startServer() throws IOException {
            Files.createDirectories(serverDir);
            String[] cmd = new String[]{node.toString(), jalangilogger.resolve("nodeJSServer/bin/www").toString()};
            Process p = exec(serverDir, cmd);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (!p.isAlive()) {
                throw new RuntimeException();
            }
            return p;
        }

        public Path log() throws IOException {
            instrument();
            Process server = startServer();
            try {
                openBrowser();
                server.waitFor();
                //waitForEnter();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                //stopServer(server);
            }
            Path log = postProcessLog(serverDir.resolve("logfile"));
            Path logWithMeta = addMeta(log, "success");
            return logWithMeta;
        }

        private void waitForEnter() throws IOException {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.printf("Press ENTER when done interacting (and pressing `p`) with the browser.%n");
            br.readLine();
        }
    }

    private class JSLogger {

        public Path log() throws IOException {
            instrument();
            String exitStatus = run();
            Path log = postProcessLog(instrumentationDir.resolve("NEW_LOG_FILE.log"));
            Path logWithMeta = addMeta(log, exitStatus);
            return logWithMeta;
        }

        private String run() throws IOException {
            Path direct_js = jalangilogger.resolve("node_modules/jalangi2").resolve("src/js/commands/direct.js").toAbsolutePath();
            String script = direct_js.toString();
            String[] cmd = {node.toString(), script, "--analysis", analysis.toString(), rootRelativeMain.toString()};
            Process p = exec(instrumentationDir, cmd);
            boolean timeout = false;
            try {
                timeout = !p.waitFor(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (timeout)
                return "timeout";
            boolean failure = p.exitValue() != 0;
            p.destroy();
            String exitStatus = failure ? "failure" : "success";
            return exitStatus;
        }
    }
}
