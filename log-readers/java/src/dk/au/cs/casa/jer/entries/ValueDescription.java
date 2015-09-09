package dk.au.cs.casa.jer.entries;

/**
 * A description of a value from a value logger
 */
public interface ValueDescription {
    <T> T accept(ValueDescriptionVisitor<T> visitor);
}
