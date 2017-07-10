package dk.au.cs.casa.jer.entries;

/**
 * An entry from the value logger
 */
public interface IEntry {
    SourceLocation getSourceLocation();

    int getIndex();

    <T> T accept(EntryVisitor<T> visitor);
}
