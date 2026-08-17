# AE2 Smart Pattern System

AE2 Smart Pattern System is a smart pattern extension for AE2 and AE2UEL.

It adds wildcard recipe expansion, item or fluid or gas marker support, recipe search, filter lists, and mod-based filtering across multiple Minecraft version tracks.

## Modern Pattern Filters

The Forge 1.20.1 and NeoForge 1.21.1 tracks support independent mod filters for
each pattern input and output:

- Each input and output can define its own mod whitelist and blacklist.
- Legacy `Excluded` data is migrated to the blacklist.
- An explicitly empty whitelist rejects all candidates; it does not mean
  "allow everything".

## Version Tracks

| Track | Loader | Status | Release |
| --- | --- | --- | --- |
| `forge-1.12.2` | Forge | Active beta warm-up | `1.0.9-beta-AE2S` |
| `forge-1.20.1` | Forge | Active migration line | `1.0.8` |
| `neoforge-1.21.1` | NeoForge | Active migration line | `1.0.8` |

## Repository

`https://github.com/wu-xiao-ya/AE2-Smart-Pattern-System`

## License

MIT
