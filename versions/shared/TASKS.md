# Migration Task Board

## Baseline

- [ ] Finalize the `1.12.2` release baseline and changelog
- [ ] Export a behavior checklist from the legacy mod
- [ ] Lock smart pattern item behavior
- [ ] Lock pattern expander interaction behavior
- [ ] Lock editor and filter GUI behavior
- [ ] Lock tooltip, localization, and sync behavior

## Common Core

- [ ] Finish immutable pattern data models
- [ ] Move non-Minecraft logic into pure Java services
- [ ] Replace direct tag access with adapter interfaces
- [ ] Add unit tests for transform and validation rules

## Forge 1.20.1

- [ ] Keep registry, content, menu, and screen behavior stable
- [ ] Finish network and saved-data migration
- [ ] Rebuild AE2 integration against the 1.20.1 API
- [ ] Validate dedicated server startup

## NeoForge 1.21.1

- [x] Bootstrap a clean NeoForge 1.21.1 workspace
- [x] Wire shared `common-core` sources into the new track
- [x] Create NeoForge bootstrap, metadata, and resource layout
- [x] Port registry and event APIs
- [ ] Port menus, screens, and payloads
- [ ] Rebuild AE2 integration against the 1.21.1 API
- [ ] Validate client and dedicated server startup

## Release Readiness

- [ ] Define artifact naming for both modern tracks
- [ ] Add CI/build matrix for Forge and NeoForge
- [ ] Write upgrade notes for players and pack authors
