# Spring Modulith Documentation

The generated Spring Modulith documentation is stored in `docs/modulith`.

Generated artifacts include:

- `docs/modulith/components.puml` - full application module component diagram
- `docs/modulith/module-*.puml` - individual module dependency diagrams
- `docs/modulith/module-*.adoc` - application module canvases
- `docs/modulith/all-docs.adoc` - aggregate Asciidoc entry point
- `docs/modulith-class-dependencies.adoc` - concrete cross-module class dependency table

Regenerate the docs with the repository toolchain:

```bash
mise exec -- mvn test -Dtest=ModuleStructureTest
```

The generator is defined in `src/test/java/biz/thonbecker/personal/modulith/ModuleStructureTest.java`.
It writes Spring Modulith output directly to `docs/modulith`, so avoid hand-editing files in that folder.
