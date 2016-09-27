package dk.au.cs.casa.jer;

import com.google.gson.Gson;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.messages.AuthConfig;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.ExecCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;

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

    private final Path jjs;

    private final Path jalangilogger;

    private final List<Path> preambles;

    private Metadata metadata;

    private final int timeLimit;

    private final Environment environment;

    /**
     * Produces a log file for the run of a single main file
     */
    public Logger(Path root, Path rootRelativeMain, List<Path> preambles, int timeLimit, Environment environment, Path node, Path jalangilogger, Path jjs) {
        this(isolateInNewRoot(root, rootRelativeMain, environment), rootRelativeMain.getParent() == null ? Paths.get("") : rootRelativeMain.getParent(), rootRelativeMain, preambles, timeLimit, environment, node, jalangilogger, jjs, initMeta(root, rootRelativeMain));
    }
    public Logger(Path root, Path rootRelativeTestDir, Path rootRelativeMain, List<Path> preambles, int timeLimit, Environment environment, Path node, Path jalangilogger, Path jjs) {
        this(root, rootRelativeTestDir, rootRelativeMain, preambles, timeLimit, environment, node, jalangilogger, jjs, initMeta(root, rootRelativeTestDir));
    }
    /**
     * Produces a log file for the run of a main file in a directory
     */
    public Logger(Path root, Path rootRelativeTestDir, Path rootRelativeMain, List<Path> preambles, int timeLimit, Environment environment, Path node, Path jalangilogger, Path jjs, Metadata metadata) {
        if(rootRelativeTestDir.isAbsolute()){
            throw new IllegalArgumentException("rootRelativeTestDir must be relative");
        }
        if(rootRelativeMain.isAbsolute()){
            throw new IllegalArgumentException("rootRelativeMain must be relative");
        }

        if (environment == Environment.BROWSER && isJsFile(rootRelativeMain)) {
            this.rootRelativeMain = createHTMLWrapper(root, rootRelativeTestDir, rootRelativeMain.getFileName(), preambles);
        } else {
            this.rootRelativeMain = rootRelativeMain;
        }

        checkAbsolutePreambles(preambles);
        this.metadata = metadata;
        this.jjs = jjs;
        this.environment = environment;
        this.timeLimit = timeLimit;
        this.preambles = preambles;
        this.root = root;
        this.rootRelativeTestDir = rootRelativeTestDir;
        this.node = node;
        this.jalangilogger = jalangilogger;
        try {
            this.temp = createTempDirectory().toRealPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.instrumentationDir = temp.resolve("instrumentation");
        this.analysis = jalangilogger.resolve("logger/src/ValueLogger.js").toAbsolutePath();
    }

    private void checkAbsolutePreambles(List<Path> preambles) {
        for (Path preamble : preambles) {
            if (!preamble.isAbsolute()) {
                throw new IllegalArgumentException(format("Preambles must be absolute %s", preamble));
            }
        }
    }

    private static Path createTempDirectory() throws IOException {
        return Files.createTempDirectory(Logger.class.getCanonicalName());
    }

    /**
     * Isolates a single file in a new empty directory. Relative paths behave as before.
     *
     * @return the new root directory
     */
    private static Path isolateInNewRoot(Path root, Path rootRelativeMain, Environment environment) {
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

    private static Path createHTMLWrapper(Path root, Path rootRelativeTestDir, Path jsFileName, List<Path> preambles) {
        List<Path> scriptSources = new ArrayList<>();
        scriptSources.addAll(preambles);
        scriptSources.add(jsFileName);
        List<String> HTMLWrap = new ArrayList<>();
        HTMLWrap.addAll(Arrays.asList(
                "<!DOCTYPE html>",
                "<html>",
                "<head>",
                "</head>",
                "<body>"
        ));
        HTMLWrap.addAll(scriptSources.stream()
                .map(source -> String.format("<script src=\"%s\"></script>", source))
                .collect(Collectors.toList())
        );
        HTMLWrap.addAll(Arrays.asList(
                "</body>",
                "</html>"));
        Path htmlWrapperRelative = rootRelativeTestDir.resolve("wrapper.html");
        Path htmlWrapper = root.resolve(htmlWrapperRelative);
        try {
            Files.write(htmlWrapper, HTMLWrap, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return htmlWrapperRelative;
    }

    private static boolean isJsFile(Path rootRelativeMain) {
        return rootRelativeMain.getFileName().toString().endsWith(".js");
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

    private Path addMeta(Path log, String result) throws IOException {
        this.metadata.setResult(result);
        Gson gson = new Gson();
        String metaJson = gson.toJson(this.metadata.jsonRep);

        List<String> lines = Files.readAllLines(log);
        lines.add(0, metaJson);
        Path outputLog = log.getParent().resolve(log.getFileName().toString() + ".metaed");
        Files.write(outputLog, lines);
        return outputLog;
    }

    private static Metadata initMeta(Path root, Path shaRoot) {
        Metadata metadata = new Metadata();
        metadata.setTime(System.currentTimeMillis());
        metadata.setRoot(shaRoot.toString());
        String hash = HashUtil.shaDirOrFile(root.resolve(shaRoot));
        metadata.setSha(hash);
        return metadata;
    }

    private Process exec(Path pwd, String... cmd) throws IOException {
        return exec(pwd, false, cmd);
    }

    private Process exec(Path pwd, boolean redirectOutput, String... cmd) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if(redirectOutput) {
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        }
        if (pwd != null) {
            pb.directory(pwd.toFile());
        }
        //pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        System.out.printf("Starting (at %s): %s%n", pwd, String.join(" ", Arrays.asList(cmd)));
        Process p = pb.start();
        return p;
    }

    public Path log() throws IOException {
        if (environment == Environment.NASHORN || environment == Environment.NODE || environment == Environment.NODE_GLOBAL) {
            return new JSLogger(environment).log();
        } else if (environment == Environment.BROWSER) {
            return new HTMLLogger().log();
        } else if (environment == Environment.DRIVEN_BROWSER) {
            return new DrivenHTMLLogger().log();
        }
        throw new IllegalArgumentException("Unsupported environment: " + environment);
    }

    private void instrument(Environment environment) throws IOException, InstrumentationSyntaxErrorException {
        Path instrument_js = jalangilogger.resolve("node_modules/jalangi2").resolve("src/js/commands/instrument.js").toAbsolutePath();
        String script = instrument_js.toString();
        String out = instrumentationDir.toAbsolutePath().toString();
        String in = ".";
        ArrayList<String> cmd = new ArrayList<>(Arrays.asList(node.toString(), script, "--inlineIID", "--analysis", analysis.toString(), "--outputDir", out));
        switch (environment) {
            case DRIVEN_BROWSER:
            case BROWSER:
                cmd.add("--instrumentInline");
                cmd.add("--inlineJalangi");
                break;
            case NASHORN:
                cmd.add("--inlineJalangiAndAnlysesInSingleJSFile");
                break;
            case NODE:
            case NODE_GLOBAL:
                break;
        }
        cmd.add(in);
        Process exec = exec(root, cmd.toArray(new String[]{}));
        try {
            exec.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        String err;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exec.getInputStream()))) {
            err = reader.lines().collect(Collectors.joining("\n"));
        }
        if (exec.exitValue() != 0) {
            throw new RuntimeException("Instrumentation failed");
        }
        if (err.contains("SyntaxError")) {
            throw new InstrumentationSyntaxErrorException();
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
            try {
                instrument(Environment.BROWSER);
            } catch (InstrumentationSyntaxErrorException e) {
                Path log = createEmptyLog();
                Path logWithMeta = addMeta(log, "syntax error");
                return logWithMeta;
            }
            Process server = startServer();
            try {
                System.out.printf("Press 'p' in the browser when done interacting with the application.%n");
                openBrowser();
                server.waitFor();
                //waitForEnter();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                //stopServer(server);
            }
            Path log = postProcessLog(serverDir.resolve("logfile"));
            Path logWithMeta = addMeta(log, "success"); // XXX we do not know that?!
            return logWithMeta;
        }

        private void waitForEnter() throws IOException {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.printf("Press ENTER when done interacting (and pressing `p`) with the browser.%n");
            br.readLine();
        }
    }

    private class DrivenHTMLLogger {

        final HostConfig hf = HostConfig.builder()
                .binds(
                        instrumentationDir.toAbsolutePath().toString() + ":/wkd",
                        jalangilogger.toAbsolutePath().toString() + ":/jalangilogger"
                ).build();

        private void checkAllCanBeShared(DockerClient docker) throws Exception {
            final ContainerConfig containerConfig = ContainerConfig.builder()
                    .image("busybox:latest")
                    .hostConfig(hf)
                    .cmd("sh", "-c", "ls /wkd && ls /jalangilogger")
                    .build();
            final ContainerCreation creation = docker.createContainer(containerConfig);
            String id = creation.id();
            docker.startContainer(id);
            if(docker.waitContainer(id).statusCode() != 0) {
                String msg = String.format("Check the docker configuration, both %s and %s need to be among the 'shared folders'", instrumentationDir.toAbsolutePath().toString(), jalangilogger.toAbsolutePath().toString());
                throw new RuntimeException(msg);
            }
        }

        String mkCmd(String jalangidir, String wd) {
            return String.format("cd '%s/browser_driver' && scripts/container/cycle '%s' '%s'", jalangidir, wd, rootRelativeMain.toString());
        }

        private void runInDocker() throws Exception {
            // Create a client based on DOCKER_HOST and DOCKER_CERT_PATH env vars
            final DockerClient docker = DefaultDockerClient.fromEnv().build();

            checkAllCanBeShared(docker);

            String imageName = "algobardo/tajsound:latest";

            // Pull an image
            //docker.pull(imageName);

            String cmd = mkCmd("/jalangilogger", "/wkd");

            final ContainerConfig containerConfig = ContainerConfig.builder()
                    .image(imageName)
                    .hostConfig(hf)
                    .cmd("bash", "-c", cmd)
                    .attachStderr(true)
                    .attachStdout(true)
                    .build();

            final ContainerCreation creation = docker.createContainer(containerConfig);

            final String id = creation.id();

            // Start container
            docker.startContainer(id);

            docker.waitContainer(id);

            System.out.println("Docker log:");
            System.out.println(docker.logs(id,
                    DockerClient.LogsParam.stdout(),
                    DockerClient.LogsParam.stderr())
                    .readFully());

            // Remove container
            docker.removeContainer(id);

            // Close the docker client
            docker.close();
        }

        public DrivenHTMLLogger() { }

        private boolean isDockerEnvironment() {
            return new File("/.dockerenv").exists();
        }

        private void captureLog() {
            try {
                Process p = exec(jalangilogger.resolve("browser_driver"), true, "bash", "-c", mkCmd(jalangilogger.toAbsolutePath().toString(), instrumentationDir.toAbsolutePath().toString()));
                Thread.sleep(1000);
                if (!p.isAlive()) {
                    throw new RuntimeException();
                }
                int res = p.waitFor();
                if (res != 0) throw new RuntimeException("Failed generating log\n" + p.getErrorStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public Path log() throws IOException {
            try {
                instrument(Environment.DRIVEN_BROWSER);
            } catch (InstrumentationSyntaxErrorException e) {
                Path log = createEmptyLog();
                Path logWithMeta = addMeta(log, "syntax error");
                return logWithMeta;
            }

            try {
                if(isDockerEnvironment()) {
                    captureLog();
                }
                else {
                    runInDocker();
                }
            }
            catch(Exception e) {
                throw new RuntimeException(e);
            }

            Path log = postProcessLog(instrumentationDir.resolve("NEW_LOG_FILE.log"));
            Path logWithMeta = addMeta(log, "success"); // XXX we do not know that?!
            return logWithMeta;
        }
    }

    private class JSLogger {

        private final Environment environment;

        private JSLogger(Environment environment) {
            this.environment = environment;
        }

        public Path log() throws IOException {
            try {
                instrument(environment);
            } catch (InstrumentationSyntaxErrorException e) {
                Path log = createEmptyLog();
                Path logWithMeta = addMeta(log, "syntax error");
                return logWithMeta;
            }
            String exitStatus = run();
            Path log = postProcessLog(instrumentationDir.resolve("NEW_LOG_FILE.log"));
            Path logWithMeta = addMeta(log, exitStatus);
            return logWithMeta;
        }

        private String run() throws IOException {
            List<String> cmd;
            switch (environment){
                case NODE:
                case NODE_GLOBAL:
                Path direct_js = jalangilogger.resolve("node_modules/jalangi2").resolve("src/js/commands/direct.js").toAbsolutePath();
                    String script = direct_js.toString();
                    Path commandLineMain = environment == Environment.NODE? rootRelativeMain: makeGlobalifier(rootRelativeMain);
                    cmd = new ArrayList<>(Arrays.asList(new String[] {node.toString(), script, "--analysis", analysis.toString(), commandLineMain.toString()}));
                    break;
                case NASHORN:
                    cmd = new ArrayList<>(Arrays.asList(new String[] {jjs.toString(), rootRelativeMain.toString(), "--"}));
                    break;
                default:
                    throw new UnsupportedOperationException("Unhandled environment kind: " + environment);
            }
            addPreambles(cmd);
            Process p = exec(instrumentationDir, cmd.toArray(new String[cmd.size()]));
            boolean timeout;
            try {
                timeout = !p.waitFor(timeLimit, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    // XXX an attempt to kill zombie nodejs-processes for good.
                    p.destroy();
                    p.waitFor(1, TimeUnit.SECONDS);
                    p.destroyForcibly();
                    p.waitFor(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    // ignore
                } finally {
                    if (p.isAlive()) {
                        throw new IllegalStateException("Could not kill process!?!");
                    }
                }
            }
            if (timeout)
                return "timeout";
            boolean failure = p.exitValue() != 0;
            p.destroy();
            String exitStatus = failure ? "failure" : "success";
            return exitStatus;
        }

        /**
         * Makes a wrapper-file that loads the main-file in a global context (instead of the node-module context)
         */
        private Path makeGlobalifier(Path mainFile) {
            try {
                Path globalifier = Files.createTempFile("globalifier", ".js");
                Files.write(globalifier, Arrays.asList(
                        "var fs = require('fs');",
                        "var globalEval = eval;",
                        String.format("(globalEval)(fs.readFileSync('%s', 'utf-8'));", mainFile.toString())
                ), StandardOpenOption.TRUNCATE_EXISTING);
                return globalifier;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Path createEmptyLog() {
        try {
            return File.createTempFile("NEW_LOG_FILE", ".log").toPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addPreambles(List<String> cmd) {
        for (Path preamble : preambles) {
            cmd.add("--preamble");
            cmd.add(preamble.toString());
        }
    }

    private class InstrumentationSyntaxErrorException extends Exception {

    }

    public enum Environment {
        NODE,
        NODE_GLOBAL,
        NASHORN,
        BROWSER,
        DRIVEN_BROWSER
    }
}
