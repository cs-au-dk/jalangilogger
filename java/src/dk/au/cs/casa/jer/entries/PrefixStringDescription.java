package dk.au.cs.casa.jer.entries;

public class PrefixStringDescription extends StringDescription {

    public PrefixStringDescription(String string) {
        super(string);
    }

    @Override
    public <T> T accept(ValueDescriptionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "PREFIX('" + escape(getString()) + "')";
    }
}
