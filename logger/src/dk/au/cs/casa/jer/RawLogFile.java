package dk.au.cs.casa.jer;

import java.util.List;

public class RawLogFile {

    private final List<String> lines;

    public RawLogFile(List<String> lines) {
        this.lines = lines;
    }

    public List<String> getLines() {
        return lines;
    }
}
