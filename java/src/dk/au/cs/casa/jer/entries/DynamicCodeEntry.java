package dk.au.cs.casa.jer.entries;

public class DynamicCodeEntry extends Entry {
    private final String source;

    DynamicCodeEntry (int index, SourceLocation sourceLocation, String source) {
        super(index, sourceLocation);
        this.source = source;
    }

    @Override
    public <T> T accept (EntryVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public String getSource () {
        return source;
    }
}
