package biz.thonbecker.personal.modulith;

import biz.thonbecker.personal.PersonalApplication;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModuleDependency;
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
     * Generates a concrete class-level report for cross-module connections.
     * Spring Modulith diagrams intentionally stay at module level, so this report complements
     * the generated diagrams with the specific source and target classes behind each dependency.
     */
    @Test
    void createsCrossModuleClassDependencyDocumentation() throws IOException {
        final var output = new StringBuilder();

        output.append("= Cross-Module Class Dependencies\n\n");
        output.append("This report is generated from Spring Modulith and ArchUnit metadata. ");
        output.append("It lists concrete classes that connect one application module to another.\n\n");

        modules.forEach(module -> appendModuleDependencies(output, module));

        Files.createDirectories(Path.of("docs"));
        Files.writeString(Path.of("docs/modulith-class-dependencies.adoc"), output.toString());
    }

    /**
     * Generates GitHub-renderable Mermaid graphs for concrete class connections.
     * These are more useful for browsing than the default module-only PlantUML diagrams.
     */
    @Test
    void createsCrossModuleClassDependencyGraphs() throws IOException {
        final var dependencies = modules.stream()
                .flatMap(module -> getModuleDependencies(module).stream())
                .toList();

        Files.createDirectories(Path.of("docs"));
        Files.writeString(Path.of("docs/modulith-class-dependencies.md"), createClassDependencyMermaid(dependencies));
        Files.writeString(Path.of("docs/modulith-event-flow.md"), createEventFlowMermaid(dependencies));
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

    private static void appendModuleDependencies(final StringBuilder output, final ApplicationModule module) {
        final var dependencies = getModuleDependencies(module);

        output.append("== ").append(module.getDisplayName()).append("\n\n");

        if (dependencies.isEmpty()) {
            output.append("No direct cross-module class dependencies.\n\n");
            return;
        }

        output.append("[%autowidth.stretch, cols=\"1,2,1,1,2,1\"]\n");
        output.append("|===\n");
        output.append(
                "|Source class |Source package |Target module |Target class |Target package |Dependency types\n\n");

        dependencies.forEach(dependency -> {
            output.append("|`").append(dependency.sourceType()).append("`\n");
            output.append("|`").append(dependency.sourcePackage()).append("`\n");
            output.append("|").append(dependency.targetModule()).append("\n");
            output.append("|`").append(dependency.targetType()).append("`\n");
            output.append("|`").append(dependency.targetPackage()).append("`\n");
            output.append("|`").append(dependency.dependencyTypes()).append("`\n\n");
        });

        output.append("|===\n\n");
    }

    private static List<ClassDependencyRow> getModuleDependencies(final ApplicationModule module) {
        return module.getDirectDependencies(modules).stream()
                .collect(Collectors.groupingBy(
                        dependency -> ClassDependencyKey.from(module.getDisplayName(), dependency),
                        Collectors.mapping(
                                dependency -> dependency.getDependencyType().name(),
                                Collectors.toCollection(TreeSet::new))))
                .entrySet()
                .stream()
                .map(entry -> ClassDependencyRow.from(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(ClassDependencyRow::targetModule)
                        .thenComparing(ClassDependencyRow::sourceModule)
                        .thenComparing(ClassDependencyRow::sourceType)
                        .thenComparing(ClassDependencyRow::targetType)
                        .thenComparing(ClassDependencyRow::dependencyTypes))
                .toList();
    }

    private static String createClassDependencyMermaid(final List<ClassDependencyRow> dependencies) {
        final var output = new StringBuilder();

        output.append("# Cross-Module Class Dependency Graph\n\n");
        output.append("Generated from Spring Modulith and ArchUnit metadata.\n\n");
        output.append("```mermaid\n");
        output.append("flowchart LR\n");

        dependencies.stream()
                .flatMap(dependency -> List.of(
                        new MermaidNode(
                                dependency.sourceModule(),
                                dependency.sourceNodeId(),
                                dependency.sourceType(),
                                dependency.sourcePackage()),
                        new MermaidNode(
                                dependency.targetModule(),
                                dependency.targetNodeId(),
                                dependency.targetType(),
                                dependency.targetPackage()))
                        .stream())
                .distinct()
                .collect(
                        Collectors.groupingBy(MermaidNode::module, TreeMap::new, Collectors.toCollection(TreeSet::new)))
                .forEach((module, nodes) -> {
                    output.append("  subgraph ")
                            .append(mermaidId(module))
                            .append("[\"")
                            .append(module)
                            .append("\"]\n");
                    nodes.forEach(node -> output.append("    ")
                            .append(node.id())
                            .append("[\"")
                            .append(node.type())
                            .append("<br/>")
                            .append(shortPackage(node.packageName()))
                            .append("\"]\n"));
                    output.append("  end\n");
                });

        dependencies.forEach(dependency -> output.append("  ")
                .append(dependency.sourceNodeId())
                .append(" -->|\"")
                .append(dependency.dependencyTypes())
                .append("\"| ")
                .append(dependency.targetNodeId())
                .append("\n"));

        output.append("```\n\n");

        return output.toString();
    }

    private static String createEventFlowMermaid(final List<ClassDependencyRow> dependencies) {
        final var eventDependencies = dependencies.stream()
                .filter(dependency -> dependency.dependencyTypes().contains("EVENT_LISTENER"))
                .toList();
        final var output = new StringBuilder();

        output.append("# Cross-Module Event Flow\n\n");
        output.append("Generated from Spring Modulith and ArchUnit event listener metadata.\n\n");
        output.append("```mermaid\n");
        output.append("flowchart LR\n");

        eventDependencies.stream()
                .map(dependency -> new MermaidNode(
                        dependency.targetModule(),
                        dependency.targetNodeId(),
                        dependency.targetType(),
                        dependency.targetPackage()))
                .distinct()
                .collect(
                        Collectors.groupingBy(MermaidNode::module, TreeMap::new, Collectors.toCollection(TreeSet::new)))
                .forEach((module, nodes) -> {
                    output.append("  subgraph ")
                            .append(mermaidId(module))
                            .append("_events[\"")
                            .append(module)
                            .append(" Events\"]\n");
                    nodes.forEach(node -> output.append("    ")
                            .append(node.id())
                            .append("[\"")
                            .append(node.type())
                            .append("\"]\n"));
                    output.append("  end\n");
                });

        eventDependencies.stream()
                .map(dependency -> new MermaidNode(
                        dependency.sourceModule(),
                        dependency.sourceNodeId(),
                        dependency.sourceType(),
                        dependency.sourcePackage()))
                .distinct()
                .collect(
                        Collectors.groupingBy(MermaidNode::module, TreeMap::new, Collectors.toCollection(TreeSet::new)))
                .forEach((module, nodes) -> {
                    output.append("  subgraph ")
                            .append(mermaidId(module))
                            .append("_listeners[\"")
                            .append(module)
                            .append(" Listeners\"]\n");
                    nodes.forEach(node -> output.append("    ")
                            .append(node.id())
                            .append("[\"")
                            .append(node.type())
                            .append("\"]\n"));
                    output.append("  end\n");
                });

        eventDependencies.forEach(dependency -> output.append("  ")
                .append(dependency.targetNodeId())
                .append(" --> ")
                .append(dependency.sourceNodeId())
                .append("\n"));

        output.append("```\n\n");

        return output.toString();
    }

    private static String mermaidId(final String value) {
        return value.replaceAll("[^A-Za-z0-9]+", "_");
    }

    private static String shortPackage(final String packageName) {
        return packageName.replace("biz.thonbecker.personal.", "");
    }

    private record ClassDependencyKey(
            String sourceModule,
            String sourceType,
            String sourcePackage,
            String targetModule,
            String targetType,
            String targetPackage) {

        private static ClassDependencyKey from(
                final String sourceModule, final ApplicationModuleDependency dependency) {
            final var source = dependency.getSourceType();
            final var target = dependency.getTargetType();

            return new ClassDependencyKey(
                    sourceModule,
                    source.getSimpleName(),
                    source.getPackageName(),
                    dependency.getTargetModule().getDisplayName(),
                    target.getSimpleName(),
                    target.getPackageName());
        }
    }

    private record ClassDependencyRow(
            String sourceModule,
            String sourceType,
            String sourcePackage,
            String targetModule,
            String targetType,
            String targetPackage,
            String dependencyTypes) {

        private static ClassDependencyRow from(
                final ClassDependencyKey dependency, final Collection<String> dependencyTypes) {
            return new ClassDependencyRow(
                    dependency.sourceModule(),
                    dependency.sourceType(),
                    dependency.sourcePackage(),
                    dependency.targetModule(),
                    dependency.targetType(),
                    dependency.targetPackage(),
                    String.join(", ", dependencyTypes));
        }

        private String sourceNodeId() {
            return mermaidId(sourcePackage + "." + sourceType);
        }

        private String targetNodeId() {
            return mermaidId(targetPackage + "." + targetType);
        }
    }

    private record MermaidNode(String module, String id, String type, String packageName)
            implements Comparable<MermaidNode> {

        @Override
        public int compareTo(final MermaidNode that) {
            return Comparator.comparing(MermaidNode::module)
                    .thenComparing(MermaidNode::type)
                    .thenComparing(MermaidNode::packageName)
                    .compare(this, that);
        }
    }
}
