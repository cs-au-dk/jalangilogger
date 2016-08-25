package dk.au.cs.casa.jer.entries;

public abstract class Entry implements IEntry {

    private final SourceLocation sourceLocation;

    public Entry(SourceLocation sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }
}
