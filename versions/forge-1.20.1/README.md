# Forge 1.20.1 路线 / Forge 1.20.1 Track

当前状态：可编译、可打开基础编辑器 GUI、可保存基础样板数据，且已接入 AE2 官方 API 解码器。  
Current status: compilable, basic editor GUI opens, pattern data persists, and AE2 official API decoder is integrated.

## 已完成 / Done

- [x] 模组引导与元数据。 / Mod bootstrap and metadata.
- [x] 物品与方块注册。 / Item and block registration.
- [x] 方块实体（18 槽）迁移。 / Block entity migration (18 slots).
- [x] 菜单与界面迁移（基础版）。 / Menu and screen migration (basic).
- [x] 样板 NBT 回写（打开读入、关闭写回）。 / Pattern NBT round-trip (load on open, save on close).
- [x] 基础 Tooltip 与本地化。 / Basic tooltip and localization.
- [x] 基础资源（model/blockstate/loot/recipes）。 / Basic assets (model/blockstate/loot/recipes).
- [x] AE2 软依赖接入（15.4.10）与解码器注册。 / AE2 soft integration (15.4.10) and decoder registration.
- [ ] AE2 自动下单执行链深度迁移。 / Deep migration of AE2 crafting execution chain.
- [ ] 黑白名单模式与流体/气体标记迁移。 / Filter mode and fluid/gas marker migration.

## 构建命令 / Build Commands

推荐使用本地脚本（已固定 Java 17 路径）：  
Use the local script (Java 17 path is pinned):

```bat
cd migration\forge-1.20.1
build-dev.bat build --console=plain
```

启动客户端：  
Run client:

```bat
cd migration\forge-1.20.1
build-dev.bat runClient --console=plain
```

Safe dev run (temporarily disables incompatible AE2/GuideME/JEI jars in `run/mods` and restores them after run):

```bat
cd migration\forge-1.20.1
run-dev-safe.bat runClient --no-daemon
```

```bat
cd migration\forge-1.20.1
run-dev-safe.bat runServer --no-daemon
```

鏃ュ父寮€鍙戞祴璇曢粯璁や笉鍔犺浇 AE2 杩愯鏃讹紙閬垮厤宸叉柟 AE2 + Forge dev Mixin 鍐茬獊锛夈€? 
Default dev run does not load AE2 runtime jars to avoid AE2 + Forge-dev mixin conflicts.

濡傞渶鍚敤 AE2 杩愯鏃讹紝鍙樉寮忎紶鍏ュ弬鏁帮細  
To enable AE2 runtime explicitly:

```bat
cd migration\forge-1.20.1
build-dev.bat -PenableAe2Runtime=true runClient --console=plain
```

## Progress Note

- Added JEI ghost ingredient support for `PatternEditorScreen` (drag ingredient into pattern slots).
- Added server sync packet for marker placement to keep client/server slots consistent.
- Migrated `modid` and resource namespace from `sampleintegration` to `techstart`.
- Added runtime filter application in `TechStartPatternDetails` decode path (whitelist/blacklist now affects decoded inputs/outputs).
- Added item-marker decode cleanup so `TechStartItemMarker` tags are stripped before creating AE2 item keys.
- Added offhand/main inventory sync reinforcement after saving pattern NBT in editor menu.
- Added `run-dev-safe.bat` to automate temporary runtime-jar disable/restore for dev launch stability.

## AE2 对齐说明 / AE2 Alignment Notes

- 依赖源使用 AE2 官方文档给出的坐标体系与仓库。  
  Dependency coordinates/repository follow AE2 official documentation.
- 当前接入版本：`appeng:appliedenergistics2-forge:15.4.10`（软依赖）。  
  Current integrated version: `appeng:appliedenergistics2-forge:15.4.10` (soft dependency).
- 已实现 `IPatternDetailsDecoder` + `IPatternDetails`，让 Pattern Provider 可识别 TechStart 样板。  
  Implemented `IPatternDetailsDecoder` + `IPatternDetails` so Pattern Provider can decode TechStart patterns.
