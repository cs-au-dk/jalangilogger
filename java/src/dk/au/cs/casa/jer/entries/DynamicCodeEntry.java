package dk.au.cs.casa.jer.entries;

public class DynamicCodeEntry extends Entry {
    private final String code;

    DynamicCodeEntry (int index, SourceLocation sourceLocation, String code) {
        super(index, sourceLocation);
        this.code = code;
    }

    @Override
    public <T> T accept (EntryVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public String getCode () {
        return code;
    }
}
