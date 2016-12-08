package dk.au.cs.casa.jer.entries;

public abstract class StringDescription implements ValueDescription {

    private final String string;

    public StringDescription(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }
}
