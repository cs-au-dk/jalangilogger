package dk.au.cs.casa.jer.entries;

import java.util.List;

public class CallEntry extends Entry {

    private final ValueDescription function;
    private final ValueDescription base;
    private final List<ValueDescription> arguments;

    public CallEntry(SourceLocation sourceLocation, ValueDescription function, ValueDescription base, List<ValueDescription> arguments) {
        super(sourceLocation);
        this.function = function;
        this.base = base;
        this.arguments = arguments;
    }

    public ValueDescription getFunction() {
        return function;
    }
    
    public ValueDescription getBase(){
    	return base;
    }
    
    public List<ValueDescription> getArguments(){
    	return arguments;
    }

    @Override
    public <T> T accept(EntryVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString () {
        return "CallEntry{" +
                "function=" + function +
                ", base=" + base +
                ", arguments=" + arguments +
                '}';
    }
}
