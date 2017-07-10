package dk.au.cs.casa.jer.entries;

public class VariableOrPropertyEntry extends Entry {

    private final ValueDescription valueDescription;

    private final ValueDescription base;

    private final ValueDescription varOrProp;

    public VariableOrPropertyEntry(int index, SourceLocation source_location, ValueDescription varOrProp, ValueDescription base, ValueDescription value) {
        super(index, source_location);
        this.valueDescription = value;
        this.base = base;
        this.varOrProp = varOrProp;
    }

    public ValueDescription getVarOrProp() {
        return varOrProp;
    }

    public ValueDescription getBase() {
        return base;
    }

    public ValueDescription getValueDescription() {
        return valueDescription;
    }

    @Override
    public <T> T accept(EntryVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "VariableOrPropertyEntry{" +
                "valueDescription=" + valueDescription +
                "base=" + base +
                ", varOrProp=" + varOrProp +
                '}';
    }
}
