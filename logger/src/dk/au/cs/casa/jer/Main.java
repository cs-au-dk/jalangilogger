package dk.au.cs.casa.jer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * Created by torp on 25/08/16.
 */
public class Main {

    public static void main(String... args) throws IOException {
        Path root = Paths.get(args[0]);
        Path mainFile = Paths.get(args[1]);
        Logger logger = null;
        List<Path> preambles = new ArrayList<>();
            Path loggerDir;
        if (args.length > 4) {
            Path testFileDir = Paths.get(args[2]);
            Path node = Paths.get(args[3]);
            loggerDir = Paths.get(args[4]);
            logger = new Logger(root, testFileDir, mainFile, preambles, node, loggerDir);
        } else {
            Path node = Paths.get(args[2]);
            loggerDir = Paths.get(args[3]);
            logger = new Logger(root, mainFile, preambles, node, loggerDir);
        }
        final Path logFile = logger.log();


        Path logFileDestinationFolder = loggerDir.resolve("JalangiLogFiles").resolve(mainFile).getParent();
        Path newLogFile = getNewLogFilePath(mainFile, logFileDestinationFolder);
        copyFile(logFile, newLogFile);
        System.out.println("Log file is located at: " + newLogFile);
    }

    private static Path getNewLogFilePath(Path testFile, Path logFileDestinationFolder) {
        String testFileName = testFile.getFileName().toString();
        return logFileDestinationFolder.resolve(format("%s.log", testFileName));
    }

    private static void copyFile(Path base, Path destination) {
        try {
            Files.createDirectories(destination.getParent());
            Files.copy(base, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
