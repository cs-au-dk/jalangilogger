package dk.au.cs.casa.jer.entries;

public class SourceLocation {

    private int linenumber;

    private int columnnumber;

    private String filename;

    public SourceLocation (int linenumber, int columnnumber, String filename) {

        this.linenumber = linenumber;
        this.columnnumber = columnnumber;
        this.filename = filename;
    }

    public int getColumnnumber () {
        return columnnumber;
    }

    public void setColumnnumber (int columnnumber) {
        this.columnnumber = columnnumber;
    }

    public String getFilename () {
        return filename;
    }

    public void setFilename (String filename) {
        this.filename = filename;
    }

    public int getLinenumber () {
        return linenumber;
    }

    public void setLinenumber (int linenumber) {
        this.linenumber = linenumber;
    }
}
