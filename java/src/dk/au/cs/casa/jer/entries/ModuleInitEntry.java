package dk.au.cs.casa.jer.entries;

public class ModuleInitEntry extends Entry {

    /**
     * File name of the module.
     */
    private final String fileName;

    /**
     * Property name.
     */
    private final ValueDescription name;

    /**
     * Property value.
     */
    private final ValueDescription value;

    public ModuleInitEntry(int index, String fileName,
                           ValueDescription name, ValueDescription value) {
        super(index, null);
        this.fileName = fileName;
        this.name = name;
        this.value = value;
    }

    public String getFileName() {
        return fileName;
    }

    public ValueDescription getName() {
        return name;
    }

    public ValueDescription getValue() {
        return value;
    }

    @Override
    public <T> T accept(EntryVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "ModuleInitEntry{" +
                "fileName='" + fileName + '\'' +
                ", name=" + name +
                ", value=" + value +
                '}';
    }
}
