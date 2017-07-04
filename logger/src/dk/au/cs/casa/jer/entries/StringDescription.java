package dk.au.cs.casa.jer.entries;

public abstract class StringDescription implements ValueDescription {

    private final String string;

    public StringDescription(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StringDescription that = (StringDescription) o;

        return string != null ? string.equals(that.string) : that.string == null;
    }

    @Override
    public int hashCode() {
        return string != null ? string.hashCode() : 0;
    }

    /**
     * Escapes special characters in the given string (for pretty printing).
     * Special characters are all Unicode chars except 0x20-0x7e but including \, ", {, and }.
     */
    protected static String escape(String s) {
        if (s == null)
            return null;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    b.append("\\\"");
                    break;
                case '\\':
                    b.append("\\\\");
                    break;
                case '\b':
                    b.append("\\b");
                    break;
                case '\t':
                    b.append("\\t");
                    break;
                case '\n':
                    b.append("\\n");
                    break;
                case '\r':
                    b.append("\\r");
                    break;
                case '\f':
                    b.append("\\f");
                    break;
                case '<':
                    b.append("\\<");
                    break;
                case '>':
                    b.append("\\>");
                    break;
                case '{':
                    b.append("\\{");
                    break;
                case '}':
                    b.append("\\}");
                    break;
                default:
                    if (c >= 0x20 && c <= 0x7e)
                        b.append(c);
                    else {
                        b.append("\\u");
                        String t = Integer.toHexString(c & 0xffff);
                        for (int j = 0; j + t.length() < 4; j++)
                            b.append('0');
                        b.append(t);
                    }
            }
        }
        return b.toString();
    }
}
