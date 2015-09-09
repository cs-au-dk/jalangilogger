package dk.au.cs.casa.jer.entries;

public class SourceLocation {

    private int lineNumber;

    private int columnNumber;

    private String fileName;

    public SourceLocation (int lineNumber, int columnNumber, String fileName) {

        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.fileName = fileName;
    }

    public int getColumnNumber () {
        return columnNumber;
    }

    public void setColumnNumber (int columnNumber) {
        this.columnNumber = columnNumber;
    }

    public String getFileName () {
        return fileName;
    }

    public void setFileName (String fileName) {
        this.fileName = fileName;
    }

    public int getLineNumber () {
        return lineNumber;
    }

    public void setLineNumber (int lineNumber) {
        this.lineNumber = lineNumber;
    }
}
