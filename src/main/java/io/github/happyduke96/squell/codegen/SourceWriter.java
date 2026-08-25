package io.github.happyduke96.squell.codegen;

final class SourceWriter {

    static final String PUBLIC = "public ";
    static final String PRIVATE = "private ";
    static final String FINAL_CLASS = "final class ";

    static final String COMMA = ", ";
    static final String OPEN_PAREN = "(";
    static final String CLOSE_PAREN = ")";
    static final String OPEN_BRACE = " {";
    static final String SEMICOLON = ";";
    static final String CLASS_LITERAL = ".class";

    private static final String SUPPRESS_UNCHECKED = "@SuppressWarnings(\"unchecked\")";

    private final StringBuilder source = new StringBuilder();
    private int indent;

    SourceWriter packageDecl(String packageName) {
        return line("package " + packageName + ";").blank();
    }

    SourceWriter override() {
        return line("@Override");
    }

    SourceWriter suppressUnchecked() {
        return line(SUPPRESS_UNCHECKED);
    }

    SourceWriter returnLine(String expression) {
        return line("return " + expression + ";");
    }

    SourceWriter line(String code) {
        source.append("    ".repeat(indent)).append(code).append('\n');
        return this;
    }

    SourceWriter blank() {
        source.append('\n');
        return this;
    }

    SourceWriter begin(String header) {
        line(header);
        indent++;
        return this;
    }

    SourceWriter end() {
        indent--;
        return line("}");
    }

    @Override
    public String toString() {
        return source.toString();
    }
}
