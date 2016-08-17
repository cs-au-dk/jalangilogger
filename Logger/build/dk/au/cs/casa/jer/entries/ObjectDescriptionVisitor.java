package dk.au.cs.casa.jer.entries;

public interface ObjectDescriptionVisitor<T> {
    T visit(OtherObjectDescription o);

    T visit(AllocationSiteObjectDescription o);

    T visit(BuiltinObjectDescription o);
}
