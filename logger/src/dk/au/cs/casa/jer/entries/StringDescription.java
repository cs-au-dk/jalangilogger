package dk.au.cs.casa.jer.entries;

public abstract class StringDescription implements ValueDescription {

    private final String string;

    public StringDescription(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StringDescription that = (StringDescription) o;

        return string != null ? string.equals(that.string) : that.string == null;
    }

    @Override
    public int hashCode() {
        return string != null ? string.hashCode() : 0;
    }
}
