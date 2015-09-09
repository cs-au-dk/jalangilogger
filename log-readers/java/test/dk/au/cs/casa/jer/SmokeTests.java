package dk.au.cs.casa.jer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@RunWith(Parameterized.class)
public class SmokeTests {

    public Path path;

    @SuppressWarnings("unused")
    public SmokeTests (String name, Path path) {
        this.path = path;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> data () throws IOException {
        List<Path> logs = getLogs(new ArrayList<>(), Paths.get("../../JalangiLogFiles"));
        List<Object[]> boxedLogs = logs.stream().map(l -> new Object[]{l.getFileName().toString(), l}).collect(Collectors.toList());
        return boxedLogs;
    }

    private static List<Path> getLogs (List<Path> logs, Path dir) throws IOException {
        DirectoryStream<Path> stream = Files.newDirectoryStream(dir);
        for (Path path : stream) {
            if (path.toFile().isDirectory()) {
                getLogs(logs, path);
            } else {
                if (path.toString().endsWith(".log")) {
                    logs.add(path.toAbsolutePath());

                }
            }
        }
        return logs;
    }

    @Test
    public void parse () {
        new LogParser(path).getEntries();

    }
}
