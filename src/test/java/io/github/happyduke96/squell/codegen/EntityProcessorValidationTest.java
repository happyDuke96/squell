package io.github.happyduke96.squell.codegen;

import org.testng.annotations.Test;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;

import static org.testng.Assert.assertTrue;

/// Compiles a throwaway `@Entity` source in-process against `EntityProcessor`, to check that an
/// invalid shape is rejected with a clear diagnostic on the original method — not left to fail
/// later as a confusing error in the generated `Table` class.
public class EntityProcessorValidationTest {

    private static final class InMemorySource extends SimpleJavaFileObject {
        private final String code;

        InMemorySource(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + ".java"), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    private List<Diagnostic<? extends JavaFileObject>> compile(String className, String source) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        try {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT,
                    List.of(Files.createTempDirectory("squell-processor-test").toFile()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null,
                List.of(new InMemorySource(className, source)));
        task.setProcessors(List.of(new EntityProcessor()));
        task.call();

        return diagnostics.getDiagnostics();
    }

    @Test
    public void idNotNamedIdIsRejectedOnTheSourceMethod() {
        List<Diagnostic<? extends JavaFileObject>> diagnostics = compile("RenamedIdProbe", """
                package io.github.happyduke96.squell.codegen.probe;

                import io.github.happyduke96.squell.sql.annotation.Entity;
                import io.github.happyduke96.squell.sql.annotation.Id;

                @Entity("orders")
                public interface RenamedIdProbe {
                    @Id
                    long orderId();
                }
                """);

        assertTrue(diagnostics.stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR
                && d.getMessage(null).contains("must annotate a method named exactly `id()`")));
    }

    @Test
    public void multipleIdFieldsAreRejected() {
        List<Diagnostic<? extends JavaFileObject>> diagnostics = compile("CompositeKeyProbe", """
                package io.github.happyduke96.squell.codegen.probe;

                import io.github.happyduke96.squell.sql.annotation.Entity;
                import io.github.happyduke96.squell.sql.annotation.Id;

                @Entity("line_items")
                public interface CompositeKeyProbe {
                    @Id
                    long orderId();
                    @Id
                    long lineNumber();
                }
                """);

        assertTrue(diagnostics.stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR
                && d.getMessage(null).contains("more than one @Id field")));
    }

    @Test
    public void fieldNamedNameCollidesWithTableNameAndIsRejected() {
        List<Diagnostic<? extends JavaFileObject>> diagnostics = compile("NameCollisionProbe", """
                package io.github.happyduke96.squell.codegen.probe;

                import io.github.happyduke96.squell.sql.annotation.Entity;
                import io.github.happyduke96.squell.sql.annotation.Id;

                @Entity("things")
                public interface NameCollisionProbe {
                    @Id
                    long id();
                    String name();
                }
                """);

        assertTrue(diagnostics.stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR
                && d.getMessage(null).contains("can't be named `name()`")
                && d.getMessage(null).contains("@Column(\"name\")")));
    }

    @Test
    public void fieldNamedFieldsCollidesWithTableFieldsAndIsRejected() {
        List<Diagnostic<? extends JavaFileObject>> diagnostics = compile("FieldsCollisionProbe", """
                package io.github.happyduke96.squell.codegen.probe;

                import io.github.happyduke96.squell.sql.annotation.Entity;
                import io.github.happyduke96.squell.sql.annotation.Id;

                @Entity("forms")
                public interface FieldsCollisionProbe {
                    @Id
                    long id();
                    String fields();
                }
                """);

        assertTrue(diagnostics.stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR
                && d.getMessage(null).contains("can't be named `fields()`")
                && d.getMessage(null).contains("@Column(\"fields\")")));
    }

    @Test
    public void aValidEntityProducesNoErrors() {
        List<Diagnostic<? extends JavaFileObject>> diagnostics = compile("ValidProbe", """
                package io.github.happyduke96.squell.codegen.probe;

                import io.github.happyduke96.squell.sql.annotation.Entity;
                import io.github.happyduke96.squell.sql.annotation.Id;

                @Entity("valid_things")
                public interface ValidProbe {
                    @Id
                    long id();
                    String label();
                }
                """);

        assertTrue(diagnostics.stream().noneMatch(d -> d.getKind() == Diagnostic.Kind.ERROR));
    }
}
