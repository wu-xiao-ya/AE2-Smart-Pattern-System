# AE2SPS Smart Pattern System - NeoForge 1.21.1

This version targets Minecraft 1.21.1 on NeoForge 21.1.x and integrates with
Applied Energistics 2.

## Mod filters

Each smart pattern stores independent input and output mod rules:

- `Whitelist`: only listed mod IDs are allowed.
- `Blacklist`: listed mod IDs are blocked and unlisted IDs are allowed.
- Left-click a mod row to edit the input set.
- Right-click a mod row to edit the output set.
- The `I` and `O` mode buttons switch the corresponding rule.

Mod IDs are normalized to lowercase namespace IDs. The server accepts at most
512 IDs per side and 64 characters per ID.

The canonical NBT keys are:

- `TechStartInputModFilterMode`
- `TechStartOutputModFilterMode`
- `TechStartInputModFilterIds`
- `TechStartOutputModFilterIds`

Patterns using `ExcludedInputModIds` or `ExcludedOutputModIds` are migrated as
blacklists when loaded. Saving writes the canonical keys and removes the legacy
keys.

The recipe `FilterEntries` list remains a separate second-stage recipe filter.

## Development

Run from this directory:

```text
gradlew.bat test build --no-daemon --console=plain
```

The build includes the shared common-core main sources and its JUnit 5 tests.
