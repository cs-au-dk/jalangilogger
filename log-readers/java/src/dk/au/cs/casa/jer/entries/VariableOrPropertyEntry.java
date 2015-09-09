package dk.au.cs.casa.jer.entries;

public class VariableOrPropertyEntry extends Entry {

    private final ValueDescription valueDescription;
    private final ValueDescription varOrProp;

    public VariableOrPropertyEntry(SourceLocation source_location, ValueDescription varOrProp, ValueDescription value) {
        super(source_location);
        this.valueDescription = value;
        this.varOrProp = varOrProp;
    }

    public ValueDescription getVarOrProp() {
        return varOrProp;
    }

    public ValueDescription getValueDescription() {
        return valueDescription;
    }

    @Override
    public <T> T accept(EntryVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString () {
        return "VariableOrPropertyEntry{" +
                "valueDescription=" + valueDescription +
                ", varOrProp=" + varOrProp +
                '}';
    }
}
