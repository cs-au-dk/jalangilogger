package dk.au.cs.casa.jer.entries;

public class BuiltinObjectDescription extends ObjectDescription {
    private final String canonicalName;

    public BuiltinObjectDescription(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    @Override
    public String toString() {
        if("<the global object>".equals(canonicalName)){
            return canonicalName;
        }
        return "BuiltinObjectDescription{" +
                "canonicalName='" + canonicalName + '\'' +
                '}';
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    @Override
    public <T> T accept(ObjectDescriptionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
