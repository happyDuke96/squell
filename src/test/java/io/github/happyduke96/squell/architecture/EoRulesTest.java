package io.github.happyduke96.squell.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.testng.annotations.Test;

import java.nio.file.Path;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

/// Enforces the README's immutability and no-setters rules structurally. `codegen` is compile-time
/// tooling; `internal`/`support` adapt `java.sql.Connection`/`DataSource` — both exempt.
public class EoRulesTest {

    private static final String CODEGEN_PACKAGE = "io.github.happyduke96.squell.codegen";
    private static final String INTERNAL_PACKAGE = "io.github.happyduke96.squell.internal";
    private static final String SUPPORT_PACKAGE = "io.github.happyduke96.squell.support";

    private static final JavaClasses MAIN_CLASSES = new ClassFileImporter()
            .importPath(Path.of("build/classes/java/main"));

    @Test
    public void everyFieldOutsideCodegenIsFinal() {
        ArchRule rule = fields()
                .that().areDeclaredInClassesThat().resideOutsideOfPackage(CODEGEN_PACKAGE)
                .should().beFinal()
                .because("squell's fluent steps are immutable — a mutation returns a new instance "
                        + "instead of changing state in place");

        rule.check(MAIN_CLASSES);
    }

    @Test
    public void noPublicSettersOutsideTheJdbcAdapterPackages() {
        ArchRule rule = noMethods()
                .that().haveNameMatching("set[A-Z].*")
                .and().areDeclaredInClassesThat()
                .resideOutsideOfPackages(INTERNAL_PACKAGE, SUPPORT_PACKAGE, CODEGEN_PACKAGE)
                .should().bePublic()
                .because("squell has no mutable builders or setters — every step returns a new step")
                .allowEmptyShould(true);

        rule.check(MAIN_CLASSES);
    }
}
