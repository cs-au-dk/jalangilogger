package dk.au.cs.casa.jer;

import java.io.IOException;
import java.nio.file.*;

/**
 * Created by torp on 25/08/16.
 */
public class Main {

    public static void main(String...args) {
        Path pwd = Paths.get(args[0]);
        Path testFile = Paths.get(args[1]);
        Path node = Paths.get("node");
        Logger logger = null;
        if (args.length > 2) {
            Path testFileDir = Paths.get(args[2]);
            logger = new Logger(pwd, testFileDir, testFile, node, pwd);
        } else {
            logger = new Logger(pwd, testFile, node, pwd);
        }
        Path logFile = null;
        try {
            logFile = logger.log();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        Path logFileDestinationFolder = getLogFileDestinationFolder(pwd, testFile);
        if (!Files.exists(logFileDestinationFolder)) {
            genDestinationDirIfMission(logFileDestinationFolder);
        }
        Path newLogFile = getNewLogFilePath(testFile, logFileDestinationFolder);
        copyFile(logFile, newLogFile);
    }

    private static Path getNewLogFilePath(Path testFile, Path logFileDestinationFolder) {
        String testFileName = testFile.getFileName().toString();
        return logFileDestinationFolder.resolve(testFileName.substring(0, testFileName.indexOf('.')) + ".log");
    }

    private static Path getLogFileDestinationFolder(Path pwd, Path testFile) {
        Path testFileWOTestDir = testFile.subpath(1, testFile.getNameCount());
        return pwd.resolve("JalangiLogFiles").resolve(testFileWOTestDir.getParent());
    }

    private static void copyFile(Path base, Path destination) {
        try {
            Files.copy(base, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void genDestinationDirIfMission(Path logFileDestinationFolder) {
        try {
            Files.createDirectories(logFileDestinationFolder);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
