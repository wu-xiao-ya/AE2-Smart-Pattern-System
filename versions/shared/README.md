# Migration Workspace

This directory holds the staged porting workspace for newer loaders and game versions.

Current tracks:

- `common-core`: shared pure-Java logic that should stay loader-agnostic
- `forge-1.20.1`: active Forge migration line
- `neoforge-1.21.1`: new NeoForge migration line

Rules:

- Keep the root project as the stable `1.12.2` baseline.
- Do not mix new-loader code into the root `src/main/java`.
- Move shared behavior into `common-core` first, then adapt loader APIs per track.
- Port one subsystem at a time: registry, content, menu, screen, network, integration.

Suggested order:

1. Freeze the `1.12.2` behavior contract.
2. Keep `forge-1.20.1` as the first fully working modern reference.
3. Build `neoforge-1.21.1` from a clean workspace and reuse shared contracts.
4. Validate behavior parity against the same checklist across tracks.
