package dk.au.cs.casa.jer.entries;

/**
 * Fallback class for (yet) unhandled value descriptions
 */
public class OtherDescription implements ValueDescription {

    private final String description;

    public OtherDescription(String description) {
        if (description.equals("STR_PREFIX"))
            this.description = "STR_OTHER";
        else
            this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OtherDescription that = (OtherDescription) o;

        return description != null ? description.equals(that.description) : that.description == null;
    }

    @Override
    public int hashCode() {
        return description != null ? description.hashCode() : 0;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }

    @Override
    public <T> T accept(ValueDescriptionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
