package biz.thonbecker.personal.modulith;

import biz.thonbecker.personal.PersonalApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;
import org.springframework.modulith.docs.Documenter.DiagramOptions;

/**
 * Tests to verify and document the modular structure of the application using Spring Modulith 2.0.
 * This test suite validates module boundaries and generates comprehensive documentation.
 */
class ModuleStructureTest {

    private static final ApplicationModules modules = ApplicationModules.of(PersonalApplication.class);

    /**
     * Verifies that all modules follow the defined architectural rules.
     * This test ensures:
     * - Modules only depend on allowed dependencies
     * - No cyclic dependencies exist
     * - Package structure follows conventions
     * - jMolecules architecture annotations are respected (included automatically in 2.0)
     */
    @Test
    void verifiesModularStructure() {
        modules.verify();
    }

    /**
     * Generates comprehensive module documentation using Spring Modulith 2.0 features.
     * Creates in the docs/ folder:
     * - C4 architecture diagrams (component and container views)
     * - Module canvases (visual module summaries)
     * - Individual module diagrams
     * - Complete documentation with all available metadata
     */
    @Test
    void createsModuleDocumentation() {
        var diagramOptions = DiagramOptions.defaults().withStyle(DiagramOptions.DiagramStyle.C4);

        var documenterOptions = Documenter.Options.defaults().withOutputFolder("docs/modulith");

        new Documenter(modules, documenterOptions)
                .writeModulesAsPlantUml(diagramOptions)
                .writeIndividualModulesAsPlantUml(diagramOptions)
                .writeModuleCanvases()
                .writeDocumentation();
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
            module.getNamedInterfaces()
                    .forEach(namedInterface -> System.out.println("    - " + namedInterface.getName()));

            System.out.println("  Dependencies:");
            module.getBootstrapDependencies(modules)
                    .forEach(dep -> System.out.println("    - " + dep.getDisplayName()));

            System.out.println();
        });
    }
}
