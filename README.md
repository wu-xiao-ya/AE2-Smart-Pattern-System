# AE2SPS

AE2UEL Smart Pattern System for multiple Minecraft version tracks.

这是一个面向 AE2UEL / AE2 生态的“智能样板系统”工作区。项目当前同时维护 `1.12.2`、`1.20.1`、`1.21.1` 三条版本线，核心目标是让单个样板支持通配符展开、物品/流体/气体标记、搜索与黑白名单筛选，并逐步把新版本体验追平。

## What It Does

- 智能样板支持矿辞通配符展开，例如 `ingot* -> dust*`
- 支持物品、流体、气体标记以及数量编辑
- 样板数据跟随样板物品 NBT 保存，而不是依赖外部方块状态
- 提供搜索界面、黑白名单界面、模组筛选界面
- 支持将展开后的虚拟配方提供给 AE2 / AE2UEL 接口

## Version Tracks

| Track | Loader | Status | Notes |
| --- | --- | --- | --- |
| `forge-1.12.2` | Forge | Active / most complete | 当前主力版本线，`1.0.7` |
| `forge-1.20.1` | Forge | Migrating | 已可构建，基础编辑器、样板数据保存、AE2 解码已接入 |
| `neoforge-1.21.1` | NeoForge | Bootstrap / migrating | 已可构建，基础框架与编辑器迁移进行中 |

## License

MIT
