package dk.au.cs.casa.jer.entries;

public interface EntryVisitor<T> {
    T visit(VariableOrPropertyEntry e);

    T visit(CallEntry e);

	T visit(FunctionExitEntry functionExitEntry);

	T visit(FunctionEntry functionEntry);

    T visit (DynamicCodeEntry dynamicCodeEntry);
}
