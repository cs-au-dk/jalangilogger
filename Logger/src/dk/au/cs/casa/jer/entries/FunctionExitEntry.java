package dk.au.cs.casa.jer.entries;

public class FunctionExitEntry extends Entry {

	private ValueDescription returnValue;
	private ValueDescription exceptionValue;
	public FunctionExitEntry(SourceLocation sourceLocation, ValueDescription returnValue, ValueDescription exceptionValue) {
		super(sourceLocation);
		this.returnValue = returnValue;
		this.exceptionValue = exceptionValue;
	}

	public ValueDescription getReturnValue(){
		return returnValue;
	}
	
	public ValueDescription getException(){
		return exceptionValue;
	}
	
	@Override
	public <T> T accept(EntryVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString () {
		return "FunctionExitEntry{" +
				"returnValue=" + returnValue +
				", exceptionValue=" + exceptionValue +
				'}';
	}
}
