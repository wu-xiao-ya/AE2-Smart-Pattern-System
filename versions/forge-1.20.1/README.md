# AE2 Smart Pattern System - Forge 1.20.1

This track targets Minecraft 1.20.1 on Forge and keeps the runtime namespace
`sampleintegration` for compatibility with the existing assets and metadata.

## Current Status

- The mod bootstrap, item and block registration, pattern editor menu, basic
  editor screen, pattern NBT persistence, tooltip, and localization are
  implemented.
- The Forge 1.20.1 build includes the shared core model and its JUnit 5 tests.
- AE2 official API decoding is integrated as a soft dependency.
- Fluid and gas marker data, item markers, wildcard expansion, and recipe-level
  filter entries are supported.
- AE2 crafting execution-chain migration is still outside this track.

## Mod Filters

Each encoded pattern stores independent input and output mod filter rules.
Every side has its own mode:

- `WHITELIST`: only listed mod namespaces are allowed.
- `BLACKLIST`: listed mod namespaces are blocked.

The mod filter screen provides separate input and output mode buttons in the
sidebar. Left-click toggles the input list and right-click toggles the output
list. Row markers, tooltips, and blocked counts reflect the effective rule,
including whitelist behavior.

Canonical pattern keys are:

- `TechStartInputModFilterMode`
- `TechStartOutputModFilterMode`
- `TechStartInputModFilterIds`
- `TechStartOutputModFilterIds`

Older `ExcludedInputModIds` and `ExcludedOutputModIds` lists are read as
blacklists when canonical IDs are absent. Saving always writes canonical mode
data, removes legacy keys, and limits each side to 512 normalized IDs of at
most 64 characters.

Recipe-level filter entries remain a second, independent filter layer after
mod namespace filtering.

## Build

Run from this directory:

```bat
gradlew.bat test build --no-daemon --console=plain
```

For a development client:

```bat
build-dev.bat runClient --console=plain
```

The optional AE2 runtime is controlled by the existing
`enableAe2Runtime` Gradle property.
