package dk.au.cs.casa.jer.entries;

import java.util.List;

public class FunctionEntry extends Entry {

	private ValueDescription function;
	private List<ValueDescription> arguments;
	private ValueDescription base;
	
	public FunctionEntry(SourceLocation sourceLocation, ValueDescription function, List<ValueDescription> arguments, ValueDescription base) {
		super(sourceLocation);
		this.function = function;
		this.arguments = arguments;
		this.base = base;
	}

	public ValueDescription getFunction(){
		return function;
	}
	
	public List<ValueDescription> getArguments() {
		return arguments;
	}
	
	public ValueDescription getBase(){
		return base;
	}

	@Override
	public <T> T accept(EntryVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString () {
		return "FunctionEntry{" +
				"function=" + function +
				", arguments=" + arguments +
				", base=" + base +
				'}';
	}
}
