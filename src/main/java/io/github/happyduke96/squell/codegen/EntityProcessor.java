package io.github.happyduke96.squell.codegen;

import io.github.happyduke96.squell.sql.annotation.Column;
import io.github.happyduke96.squell.sql.annotation.Convert;
import io.github.happyduke96.squell.sql.annotation.Entity;
import io.github.happyduke96.squell.sql.annotation.GenerateTableRegistry;
import io.github.happyduke96.squell.sql.annotation.GeneratedValue;
import io.github.happyduke96.squell.sql.annotation.Id;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes({
        "io.github.happyduke96.squell.sql.annotation.Entity",
        "io.github.happyduke96.squell.sql.annotation.GenerateTableRegistry"
})
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public final class EntityProcessor extends AbstractProcessor {

    private static final String FIELD = "io.github.happyduke96.squell.condition.Field";
    private static final String CONSTRAINTS = "io.github.happyduke96.squell.condition.ColumnConstraints";
    private static final String CONVERTER = "io.github.happyduke96.squell.converter.Converter";
    private static final String NO_CONVERTER = "io.github.happyduke96.squell.converter.NoConverter";
    private static final String TABLE = "io.github.happyduke96.squell.sql.Table";
    private static final String DEFAULT_TABLE = "io.github.happyduke96.squell.sql.DefaultTable";
    private static final String LIST = "java.util.List";
    private static final String RESULT_SET = "java.sql.ResultSet";
    private static final String SQL_EXCEPTION = "java.sql.SQLException";

    private static final String TABLE_SUFFIX = "Table";
    private static final String IMPL_SUFFIX = "Impl";
    private static final String TABLES_REGISTRY_CLASS = "Tables";

    private record ColumnModel(String name,
                               String columnName,
                               String accessorType,
                               String columnType,
                               boolean isId,
                               boolean needsConverter,
                               String converterClass,
                               boolean generated,
                               boolean nonNull,
                               boolean unique,
                               boolean nonNegative,
                               ExecutableElement element) {
    }

    private record EntityInfo(String packageName, String entityName, boolean hasNoArgConstructor) {
    }

    private final List<EntityInfo> processedEntities = new ArrayList<>();
    private final Set<String> writtenRegistries = new LinkedHashSet<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Entity.class)) {
            if (element.getKind() != ElementKind.INTERFACE) {
                error(element, "@Entity can only annotate an interface.");
                continue;
            }
            try {
                EntityInfo info = writeTable((TypeElement) element);
                if (info != null) {
                    processedEntities.add(info);
                }
            } catch (IOException e) {
                error(element, "Could not write generated Table: " + e.getMessage());
            }
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(GenerateTableRegistry.class)) {
            if (element instanceof PackageElement packageElement) {
                String packageName = packageElement.getQualifiedName().toString();
                if (writtenRegistries.add(packageName)) {
                    try {
                        writeRegistry(packageName);
                    } catch (IOException e) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                "Could not write Tables registry: " + e.getMessage());
                    }
                }
            }
        }
        return true;
    }

    private EntityInfo writeTable(TypeElement entityElement) throws IOException {
        String entityName = entityElement.getSimpleName().toString();
        String tableName = entityElement.getAnnotation(Entity.class).value();
        String packageName = processingEnv.getElementUtils().getPackageOf(entityElement).getQualifiedName().toString();

        List<ColumnModel> columns = readColumns(entityElement);
        if (!validate(entityElement, columns)) {
            return null;
        }
        boolean hasNoArgConstructor = columns.stream()
                .filter(ColumnModel::needsConverter)
                .allMatch(c -> c.converterClass() != null);
        String source = render(packageName, entityName, tableName, columns, hasNoArgConstructor);

        JavaFileObject file = processingEnv.getFiler()
                .createSourceFile(packageName + "." + entityName + TABLE_SUFFIX, entityElement);
        try (PrintWriter writer = new PrintWriter(file.openWriter())) {
            writer.print(source);
        }
        return new EntityInfo(packageName, entityName, hasNoArgConstructor);
    }

    /// Column accessor names that collide with a zero-arg `Table`/`DefaultTable` method of a
    /// different return type — every other method there either takes a parameter or (`id()`)
    /// already returns `Field<ID>`, so these two are the only names that can't be generated.
    private static final Set<String> RESERVED_COLUMN_NAMES = Set.of("name", "fields");

    /// Rejects entity shapes that would compile into broken generated code: more than one
    /// `@Id`, an `@Id` not literally named `id()` (required to override `DefaultTable#id()`),
    /// or a column named `name()`/`fields()` (collides with the generated `Table` method).
    private boolean validate(TypeElement entityElement, List<ColumnModel> columns) {
        List<ColumnModel> idColumns = columns.stream().filter(ColumnModel::isId).toList();
        if (idColumns.size() > 1) {
            String names = idColumns.stream().map(ColumnModel::name).collect(Collectors.joining(", "));
            for (ColumnModel id : idColumns) {
                error(id.element(), "@Entity [" + entityElement.getSimpleName() + "] has more than one @Id "
                        + "field (" + names + ") — squell doesn't support composite primary keys. Omit @Id "
                        + "entirely instead: the generated Table implements plain Table<T> (no id()/byId()), "
                        + "which is the supported way to model a join table or composite key.");
            }
            return false;
        }
        if (idColumns.size() == 1 && !idColumns.getFirst().name().equals("id")) {
            ColumnModel id = idColumns.getFirst();
            error(id.element(), "@Id must annotate a method named exactly `id()`, not `" + id.name()
                    + "()` — DefaultTable<T, ID> requires it.");
            return false;
        }
        for (ColumnModel column : columns) {
            if (RESERVED_COLUMN_NAMES.contains(column.name())) {
                error(column.element(), "A column accessor can't be named `" + column.name() + "()` — it "
                        + "collides with the generated Table#" + column.name() + "() method. Rename the "
                        + "accessor and add @Column(\"" + column.name() + "\") to keep the actual SQL column "
                        + "name — e.g. `@Column(\"" + column.name() + "\") String label();`.");
                return false;
            }
        }
        return true;
    }

    private void writeRegistry(String packageName) throws IOException {
        List<EntityInfo> included = processedEntities.stream()
                .filter(EntityInfo::hasNoArgConstructor)
                .toList();
        for (EntityInfo skipped : processedEntities) {
            if (!skipped.hasNoArgConstructor()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                        TABLES_REGISTRY_CLASS + " registry in [" + packageName + "] excludes ["
                                + skipped.entityName() + TABLE_SUFFIX + "] — it needs at least one explicit "
                                + "converter, so it has no no-arg constructor. Add it to "
                                + "validateSchema/validateSchemaOrThrow by hand.");
            }
        }

        SourceWriter out = new SourceWriter();
        out.packageDecl(packageName);
        out.line("/// Generated by [EntityProcessor] — every processed entity whose `Table` has a no-arg "
                + "constructor. Entities needing an explicit converter are left out; see the build log.");
        out.begin(SourceWriter.PUBLIC + SourceWriter.FINAL_CLASS + TABLES_REGISTRY_CLASS + SourceWriter.OPEN_BRACE);
        out.blank();
        out.begin("private " + TABLES_REGISTRY_CLASS + SourceWriter.OPEN_PAREN + SourceWriter.CLOSE_PAREN + SourceWriter.OPEN_BRACE);
        out.end();
        out.blank();
        out.begin(SourceWriter.PUBLIC + "static " + LIST + "<" + TABLE + "<?>> all" + SourceWriter.OPEN_PAREN
                + SourceWriter.CLOSE_PAREN + SourceWriter.OPEN_BRACE);
        String entries = included.stream()
                .map(e -> "new " + e.packageName() + "." + e.entityName() + TABLE_SUFFIX + SourceWriter.OPEN_PAREN
                        + SourceWriter.CLOSE_PAREN)
                .collect(Collectors.joining(SourceWriter.COMMA));
        out.returnLine(LIST + ".of" + SourceWriter.OPEN_PAREN + entries + SourceWriter.CLOSE_PAREN);
        out.end();
        out.end();

        JavaFileObject file = processingEnv.getFiler().createSourceFile(packageName + ".Tables");
        try (PrintWriter writer = new PrintWriter(file.openWriter())) {
            writer.print(out.toString());
        }
    }

    private List<ColumnModel> readColumns(TypeElement entityElement) {
        List<ColumnModel> columns = new ArrayList<>();
        for (Element member : entityElement.getEnclosedElements()) {
            if (member.getKind() != ElementKind.METHOD) {
                continue;
            }
            ExecutableElement method = (ExecutableElement) member;
            if (!method.getParameters().isEmpty() || method.isDefault() || method.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }
            boolean generated = method.getAnnotation(GeneratedValue.class) != null;
            boolean isId = method.getAnnotation(Id.class) != null;
            String name = method.getSimpleName().toString();
            Column columnAnnotation = method.getAnnotation(Column.class);
            String columnName = (columnAnnotation != null && !columnAnnotation.value().isEmpty())
                    ? columnAnnotation.value() : name;
            boolean nonNull = isId || (columnAnnotation != null && columnAnnotation.nonNull());
            boolean unique = isId || (columnAnnotation != null && columnAnnotation.unique());
            boolean nonNegative = isId && isNumericPrimitive(method.getReturnType());
            String accessorType = method.getReturnType().toString();
            String columnType = generated ? generatedColumnType(method) : boxed(method.getReturnType());
            boolean needsConverter = method.getAnnotation(Convert.class) != null;
            columns.add(new ColumnModel(
                    name,
                    columnName,
                    accessorType,
                    columnType,
                    isId,
                    needsConverter,
                    needsConverter ? namedConverterClass(method) : null,
                    generated,
                    nonNull,
                    unique,
                    nonNegative,
                    method));
        }
        return columns;
    }

    private static boolean isNumericPrimitive(TypeMirror type) {
        return switch (type.getKind()) {
            case INT, LONG, DOUBLE, FLOAT, SHORT, BYTE -> true;
            default -> false;
        };
    }

    private String namedConverterClass(ExecutableElement method) {
        TypeMirror converterType;
        try {
            method.getAnnotation(Convert.class).value();
            throw new AssertionError("Convert.value() must throw MirroredTypeException here.");
        } catch (MirroredTypeException e) {
            converterType = e.getTypeMirror();
        }
        return converterType.toString().equals(NO_CONVERTER) ? null : converterType.toString();
    }

    private String generatedColumnType(ExecutableElement method) {
        TypeMirror returnType = method.getReturnType();
        if (returnType.getKind() == TypeKind.DECLARED) {
            DeclaredType declared = (DeclaredType) returnType;
            if (((TypeElement) declared.asElement()).getQualifiedName().contentEquals("java.util.Optional")
                    && declared.getTypeArguments().size() == 1) {
                return boxed(declared.getTypeArguments().getFirst());
            }
        }
        error(method, "@GeneratedValue accessor must return java.util.Optional<...>.");
        return boxed(returnType);
    }

    private static String boxed(TypeMirror type) {
        return switch (type.getKind()) {
            case INT -> "Integer";
            case LONG -> "Long";
            case BOOLEAN -> "Boolean";
            case DOUBLE -> "Double";
            case FLOAT -> "Float";
            case SHORT -> "Short";
            case BYTE -> "Byte";
            case CHAR -> "Character";
            default -> type.toString();
        };
    }

    private static String classLiteral(String boxed) {
        int genericStart = boxed.indexOf('<');
        return genericStart == -1 ? boxed : boxed.substring(0, genericStart);
    }

    private static String render(String packageName, String entityName, String tableName, List<ColumnModel> columns,
            boolean hasNoArgConstructor) {
        Optional<ColumnModel> idColumn = columns.stream().filter(ColumnModel::isId).findFirst();
        String implementsClause = idColumn
                .map(id -> DEFAULT_TABLE + "<" + entityName + SourceWriter.COMMA + id.columnType() + ">")
                .orElse(TABLE + "<" + entityName + ">");
        List<ColumnModel> convertedColumns = columns.stream().filter(ColumnModel::needsConverter).toList();

        SourceWriter out = new SourceWriter();
        out.packageDecl(packageName);
        out.line("/// Generated from [" + entityName + "] by [EntityProcessor] — do not edit by hand.");
        out.begin(SourceWriter.PUBLIC + SourceWriter.FINAL_CLASS + entityName + "Table implements " + implementsClause
                + SourceWriter.OPEN_BRACE);
        out.blank();

        for (ColumnModel column : columns) {
            if (column.needsConverter()) {
                out.line(SourceWriter.PRIVATE + "final " + FIELD + "<" + column.columnType() + "> " + column.name()
                        + SourceWriter.SEMICOLON);
            } else {
                out.line(SourceWriter.PRIVATE + "final " + FIELD + "<" + column.columnType() + "> " + column.name()
                        + " = new " + FIELD + "<>" + SourceWriter.OPEN_PAREN + "\"" + column.columnName() + "\""
                        + SourceWriter.COMMA + classLiteral(column.columnType()) + SourceWriter.CLASS_LITERAL
                        + ", new " + CONSTRAINTS + SourceWriter.OPEN_PAREN + column.generated() + SourceWriter.COMMA
                        + column.nonNull() + SourceWriter.COMMA + column.unique() + SourceWriter.CLOSE_PAREN
                        + SourceWriter.CLOSE_PAREN + SourceWriter.SEMICOLON);
            }
        }
        out.blank();

        if (!convertedColumns.isEmpty()) {
            String ctorParams = convertedColumns.stream()
                    .map(c -> CONVERTER + "<" + c.columnType() + ", ?> " + c.name() + "Converter")
                    .collect(Collectors.joining(SourceWriter.COMMA));
            out.suppressUnchecked();
            out.begin(SourceWriter.PUBLIC + entityName + TABLE_SUFFIX + SourceWriter.OPEN_PAREN + ctorParams
                    + SourceWriter.CLOSE_PAREN + SourceWriter.OPEN_BRACE);
            for (ColumnModel c : convertedColumns) {
                out.line(c.name() + " = new " + FIELD + "<" + c.columnType() + ">" + SourceWriter.OPEN_PAREN + "\""
                        + c.columnName() + "\"" + SourceWriter.COMMA + "(Class<" + c.columnType()
                        + ">) (Class<?>) " + classLiteral(c.columnType()) + SourceWriter.CLASS_LITERAL + ", new "
                        + CONSTRAINTS + SourceWriter.OPEN_PAREN + c.generated() + SourceWriter.COMMA + c.nonNull()
                        + SourceWriter.COMMA + c.unique() + SourceWriter.CLOSE_PAREN + SourceWriter.COMMA
                        + c.name() + "Converter" + SourceWriter.CLOSE_PAREN + SourceWriter.SEMICOLON);
            }
            out.end();
            out.blank();

            if (hasNoArgConstructor) {
                out.begin(SourceWriter.PUBLIC + entityName + TABLE_SUFFIX + SourceWriter.OPEN_PAREN
                        + SourceWriter.CLOSE_PAREN + SourceWriter.OPEN_BRACE);
                String delegateArgs = convertedColumns.stream()
                        .map(c -> "new " + c.converterClass() + SourceWriter.OPEN_PAREN + SourceWriter.CLOSE_PAREN)
                        .collect(Collectors.joining(SourceWriter.COMMA));
                out.line("this" + SourceWriter.OPEN_PAREN + delegateArgs + SourceWriter.CLOSE_PAREN
                        + SourceWriter.SEMICOLON);
                out.end();
                out.blank();
            }
        }

        for (ColumnModel column : columns) {
            String signature = SourceWriter.PUBLIC + FIELD + "<" + column.columnType() + "> " + column.name()
                    + SourceWriter.OPEN_PAREN + SourceWriter.CLOSE_PAREN + SourceWriter.OPEN_BRACE;
            if (column.isId()) {
                out.override();
            }
            out.begin(signature);
            out.returnLine(column.name());
            out.end();
            out.blank();
        }

        String constructorArgs = columns.stream()
                .filter(c -> !c.generated())
                .map(c -> c.accessorType() + " " + c.name())
                .collect(Collectors.joining(SourceWriter.COMMA));
        out.begin(SourceWriter.PUBLIC + entityName + " create" + SourceWriter.OPEN_PAREN + constructorArgs
                + SourceWriter.CLOSE_PAREN + SourceWriter.OPEN_BRACE);
        for (ColumnModel column : columns) {
            if (column.isId() && column.nonNegative()) {
                out.begin("if (" + column.name() + " < 0)" + SourceWriter.OPEN_BRACE);
                out.line("throw new IllegalArgumentException(\"[" + column.columnName()
                        + "] must not be negative, got [\" + " + column.name() + " + \"].\")" + SourceWriter.SEMICOLON);
                out.end();
            }
        }
        out.returnLine("new " + entityName + IMPL_SUFFIX + SourceWriter.OPEN_PAREN + createArgRefs(columns)
                + SourceWriter.CLOSE_PAREN);
        out.end();
        out.blank();

        out.override();
        out.begin(SourceWriter.PUBLIC + "String name" + SourceWriter.OPEN_PAREN + SourceWriter.CLOSE_PAREN
                + SourceWriter.OPEN_BRACE);
        out.returnLine("\"" + tableName + "\"");
        out.end();
        out.blank();

        out.override();
        out.begin(SourceWriter.PUBLIC + LIST + "<" + FIELD + "<?>> fields" + SourceWriter.OPEN_PAREN
                + SourceWriter.CLOSE_PAREN + SourceWriter.OPEN_BRACE);
        out.returnLine(LIST + ".of" + SourceWriter.OPEN_PAREN + fieldRefs(columns) + SourceWriter.CLOSE_PAREN);
        out.end();
        out.blank();

        out.override();
        out.suppressUnchecked();
        out.begin(SourceWriter.PUBLIC + entityName + " fromRow" + SourceWriter.OPEN_PAREN + RESULT_SET
                + " row" + SourceWriter.CLOSE_PAREN + " throws " + SQL_EXCEPTION + SourceWriter.OPEN_BRACE);
        out.returnLine("new " + entityName + IMPL_SUFFIX + SourceWriter.OPEN_PAREN + columns.stream()
                .map(c -> c.generated()
                        ? "java.util.Optional.of((" + c.columnType() + ") " + c.name() + ".readFrom(row))"
                        : "(" + c.accessorType() + ") " + c.name() + ".readFrom(row)")
                .collect(Collectors.joining(SourceWriter.COMMA)) + SourceWriter.CLOSE_PAREN);
        out.end();
        out.blank();

        out.override();
        out.begin(SourceWriter.PUBLIC + LIST + "<Object> values" + SourceWriter.OPEN_PAREN + entityName
                + " entity" + SourceWriter.CLOSE_PAREN + SourceWriter.OPEN_BRACE);
        out.returnLine("java.util.Arrays.asList" + SourceWriter.OPEN_PAREN + columns.stream()
                .map(c -> c.generated()
                        ? c.name() + ".toSqlValue(entity." + c.name() + "().orElse(null))"
                        : c.name() + ".toSqlValue(entity." + c.name() + "())")
                .collect(Collectors.joining(SourceWriter.COMMA)) + SourceWriter.CLOSE_PAREN);
        out.end();
        out.blank();

        String recordComponents = columns.stream()
                .map(c -> c.accessorType() + " " + c.name())
                .collect(Collectors.joining(SourceWriter.COMMA));
        out.line(SourceWriter.PRIVATE + "record " + entityName + IMPL_SUFFIX + SourceWriter.OPEN_PAREN
                + recordComponents + SourceWriter.CLOSE_PAREN + " implements " + entityName + SourceWriter.OPEN_BRACE);
        out.line("}");

        out.end();

        return out.toString();
    }

    private static String createArgRefs(List<ColumnModel> columns) {
        return columns.stream()
                .map(c -> c.generated() ? "java.util.Optional.empty()" : c.name())
                .collect(Collectors.joining(SourceWriter.COMMA));
    }

    private static String fieldRefs(List<ColumnModel> columns) {
        return columns.stream().map(ColumnModel::name).collect(Collectors.joining(SourceWriter.COMMA));
    }

    private void error(Element element, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
