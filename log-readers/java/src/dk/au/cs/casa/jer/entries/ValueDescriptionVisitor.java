package dk.au.cs.casa.jer.entries;

public interface ValueDescriptionVisitor<T> {
    T visit(OtherDescription d);

    T visit(ConcreteStringDescription d);

    T visit(ObjectDescription d);
}
