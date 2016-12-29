package dk.au.cs.casa.jer.entries;

public class OtherSymbolDescription extends ObjectDescription {

    private final String toStringValue;

    public OtherSymbolDescription(String toStringValue) {
        this.toStringValue = toStringValue;
    }

    public String getToStringValue() {
        return toStringValue;
    }

    @Override
    public <T> T accept(ObjectDescriptionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "OtherSymbolDescription{" +
                "toStringValue='" + toStringValue + '\'' +
                '}';
    }
}
