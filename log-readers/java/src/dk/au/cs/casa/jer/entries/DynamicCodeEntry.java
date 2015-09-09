package dk.au.cs.casa.jer.entries;

public class DynamicCodeEntry extends Entry {
    private final String source;

    DynamicCodeEntry (SourceLocation sourceLocation, String source) {
        super(sourceLocation);
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
