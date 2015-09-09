package dk.au.cs.casa.jer.entries;

public abstract class ObjectDescription implements ValueDescription {
    @Override
    public <T> T accept(ValueDescriptionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public abstract <T> T accept(ObjectDescriptionVisitor<T> visitor);
}
