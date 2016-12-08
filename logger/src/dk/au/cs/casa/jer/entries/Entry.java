package dk.au.cs.casa.jer.entries;

public abstract class Entry implements IEntry {

    private final int index;

    private final SourceLocation sourceLocation;

    public Entry(int index, SourceLocation sourceLocation) {
        this.index = index;
        this.sourceLocation = sourceLocation;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }
}
