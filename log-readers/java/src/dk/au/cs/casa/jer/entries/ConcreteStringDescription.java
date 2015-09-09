package dk.au.cs.casa.jer.entries;

public class ConcreteStringDescription implements ValueDescription {
    private final String string;

    public ConcreteStringDescription(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }

    @Override
    public <T> T accept(ValueDescriptionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "'" + string + "'";
    }
}
