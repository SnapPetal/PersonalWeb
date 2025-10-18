package biz.thonbecker.personal.modulith;

import biz.thonbecker.personal.PersonalApplication;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Tests to verify and document the modular structure of the application.
 */
class ModuleStructureTest {

    private static final ApplicationModules modules =
            ApplicationModules.of(PersonalApplication.class);

    /**
     * Verifies that all modules follow the defined architectural rules.
     * This test ensures:
     * - Modules only depend on allowed dependencies
     * - No cyclic dependencies exist
     * - Package structure follows conventions
     */
    @Test
    void verifiesModularStructure() {
        modules.verify();
    }

    /**
     * Generates comprehensive module documentation in the docs/ folder.
     * This creates:
     * - Module dependency diagrams
     * - Module component diagrams
     * - Documentation of module APIs
     */
    @Test
    void createsModuleDocumentation() {
        new Documenter(modules).writeModulesAsPlantUml().writeIndividualModulesAsPlantUml();
    }

    /**
     * Prints module structure to console for quick inspection.
     */
    @Test
    void printsModuleStructure() {
        System.out.println("\n=== Application Module Structure ===\n");
        modules.forEach(module -> {
            System.out.println("Module: " + module.getDisplayName());
            System.out.println("  Display Name: " + module.getDisplayName());
            System.out.println("  Base Package: " + module.getBasePackage().getName());

            System.out.println("  Named Interfaces:");
            module.getNamedInterfaces().forEach(namedInterface -> {
                System.out.println("    - " + namedInterface.getName());
            });

            System.out.println("  Dependencies:");
            module.getBootstrapDependencies(modules)
                    .forEach(dep -> System.out.println("    - " + dep.getDisplayName()));

            System.out.println();
        });
    }
}
