package dk.au.cs.casa.jer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by torp on 25/08/16.
 */
public class Main {

    public static void main(String... args) throws IOException {
        Logger logger;
        List<Path> preambles = new ArrayList<>();
        int defaultTimeLimit = 60;
        Set<Path> onlyInclude = new HashSet<>();
        if (args.length > 4) {
            Path mainFile = Paths.get(args[0]);
            Path node = Paths.get(args[1]);
            Path loggerDir = Paths.get(args[2]);
            logger = Logger.makeLoggerForIndependentMainFile(mainFile, preambles, onlyInclude, defaultTimeLimit, getEnvironment(mainFile), node, loggerDir, null);
        } else {
            Path root = Paths.get(args[0]);
            Path mainFile = Paths.get(args[1]);
            Path node = Paths.get(args[2]);
            Path loggerDir = Paths.get(args[3]);
            logger = Logger.makeLoggerForDirectoryWithMainFile(root, mainFile, preambles, onlyInclude, defaultTimeLimit, getEnvironment(mainFile), node, loggerDir, null);
        }
        final Path logFile = logger.log();
        System.out.println("Log file is located at: " + logFile);
    }

    private static Logger.Environment getEnvironment(Path mainFile) {
        return mainFile.getFileName().toString().endsWith(".js") ? Logger.Environment.NODE_GLOBAL : Logger.Environment.BROWSER;
    }
}
