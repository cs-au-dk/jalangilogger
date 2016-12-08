package dk.au.cs.casa.jer.entries;

public class ConcreteStringDescription extends StringDescription {

    public ConcreteStringDescription(String string) {
        super(string);
    }

    @Override
    public String toString() {
        return "'" + getString() + "'";
    }

    @Override
    public <T> T accept(ValueDescriptionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
