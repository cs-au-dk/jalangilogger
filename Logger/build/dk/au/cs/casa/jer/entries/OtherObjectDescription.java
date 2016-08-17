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
    public String toString() {
        return "OtherObjectDescription{}";
    }
}
