package dk.au.cs.casa.jer.entries;

public class SourceLocation {

    private int lineNumber;

    private int columnNumber;

    private String fileName;

    public SourceLocation(int lineNumber, int columnNumber, String fileName) {
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return String.format("%s:%d:%d", fileName, lineNumber, columnNumber);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final SourceLocation that = (SourceLocation) o;

        if (lineNumber != that.lineNumber) return false;
        if (columnNumber != that.columnNumber) return false;
        if (fileName != null ? !fileName.equals(that.fileName) : that.fileName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = lineNumber;
        result = 31 * result + columnNumber;
        result = 31 * result + (fileName != null ? fileName.hashCode() : 0);
        return result;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    public void setColumnNumber(int columnNumber) {
        this.columnNumber = columnNumber;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }
}
