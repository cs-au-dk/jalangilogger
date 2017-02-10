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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OtherSymbolDescription that = (OtherSymbolDescription) o;

        return toStringValue != null ? toStringValue.equals(that.toStringValue) : that.toStringValue == null;
    }

    @Override
    public int hashCode() {
        return toStringValue != null ? toStringValue.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "OtherSymbolDescription{" +
                "toStringValue='" + toStringValue + '\'' +
                '}';
    }
}
