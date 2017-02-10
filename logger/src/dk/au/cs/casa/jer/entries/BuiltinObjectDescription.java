package dk.au.cs.casa.jer.entries;

public class BuiltinObjectDescription extends ObjectDescription {

    private final String canonicalName;

    public BuiltinObjectDescription(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    @Override
    public String toString() {
        if ("<the global object>".equals(canonicalName)) {
            return canonicalName;
        }
        return "BuiltinObjectDescription{" +
                "canonicalName='" + canonicalName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BuiltinObjectDescription that = (BuiltinObjectDescription) o;

        return canonicalName != null ? canonicalName.equals(that.canonicalName) : that.canonicalName == null;
    }

    @Override
    public int hashCode() {
        return canonicalName != null ? canonicalName.hashCode() : 0;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    @Override
    public <T> T accept(ObjectDescriptionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
