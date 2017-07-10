package dk.au.cs.casa.jer.entries;

/**
 * Fallback class for (yet) unhandled object descriptions
 */
public class OtherObjectDescription extends ObjectDescription {

    @Override
    public <T> T accept(ObjectDescriptionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof OtherObjectDescription;
    }

    @Override
    public int hashCode() {
        return OtherObjectDescription.class.hashCode();
    }

    @Override
    public String toString() {
        return "OtherObjectDescription{}";
    }
}
