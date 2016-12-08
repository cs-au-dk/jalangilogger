package dk.au.cs.casa.jer.entries;

public class FunctionExitEntry extends Entry {

    private final ValueDescription returnValue;

    private final ValueDescription exceptionValue;

    public FunctionExitEntry(int index, SourceLocation sourceLocation, ValueDescription returnValue, ValueDescription exceptionValue) {
        super(index, sourceLocation);
        this.returnValue = returnValue;
        this.exceptionValue = exceptionValue;
    }

    public ValueDescription getReturnValue() {
        return returnValue;
    }

    public ValueDescription getException() {
        return exceptionValue;
    }

    @Override
    public <T> T accept(EntryVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "FunctionExitEntry{" +
                "returnValue=" + returnValue +
                ", exceptionValue=" + exceptionValue +
                '}';
    }
}
