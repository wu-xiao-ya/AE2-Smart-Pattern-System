# 1.12.2 -> 1.20.1 / 1.21.1 迁移审计

本文件基于当前仓库实际代码状态整理，目标是把“旧版已有功能”“Forge 1.20.1 已完成内容”“仍未迁移的缺口”放到同一张表里，方便按子系统推进。

## 当前结论

- 根项目 `src/main` 仍是稳定的 `1.12.2` 基线。
- `migration/forge-1.20.1` 已具备可编译骨架，并已实现基础内容注册、菜单、界面、网络、AE2 解码器接入。
- `migration/neoforge-1.21.1` 目前仍处于规划阶段，尚未开始代码级迁移。
- 当前最大缺口不是“能不能编译”，而是“旧版智能通配样板的核心玩法是否已在现代版本复现”。答案是：还没有完全复现。

## 模块映射总览

| 旧版 1.12.2 | 作用 | 1.20.1 对应现状 | 结论 |
|---|---|---|---|
| `TechStart` | 模组入口、注册、GUI handler、命令 | `TechStartForge` 已建立现代入口 | 已部分迁移 |
| `ItemTest` | 智能样板物品、NBT 编码、tooltip、AE2 `ICraftingPatternItem` | `PatternIntegrationsItem` 已实现现代物品和 tooltip | 已部分迁移 |
| `BlockPatternExpander` | 样板扩展器方块 | `PatternExpanderBlock` | 已迁移基础壳 |
| `TileEntityPatternExpander` | AE2 crafting provider、扫描、样板提供 | `PatternExpanderBlockEntity` 目前只做菜单容器/物品能力 | 核心缺失 |
| `ContainerPatternEditor` | 旧版服务端容器、编码逻辑、流体/气体识别 | `PatternEditorMenu` 已实现现代菜单与保存逻辑 | 已部分迁移 |
| `GuiPatternEditor` / `GuiPatternFilter` / `GuiPatternSearch` / `GuiModFilter` | 旧版 GUI 体系 | `PatternEditorScreen` 已合并大部分编辑/过滤逻辑 | 已部分迁移 |
| `Packet*` / `PacketHandler` | 旧版网络同步 | `TechStartNetwork` + `SetPatternSlotPacket` | 已迁移一小部分 |
| `SmartPatternDetails` | AE2 智能样板细节、通配扩展、流体/气体 | `TechStartPatternDetails` 仅做现代解码与过滤 | 关键语义缺失 |
| `OreDictRecipeCache` | OreDictionary 通配配方缓存 | 暂无直接替代 | 缺失 |
| `PatternInterceptor` + ASM/Mixin | 注入 AE2 接口样板列表 | 1.20.1 暂无等价机制 | 缺失 |
| `ModConfig` | 通配前缀、显式 ore 对、扫描周期等配置 | 1.20.1 未见对应配置实现 | 缺失 |
| `JEIIntegrationPlugin` | JEI 集成 | `TechStartJeiPlugin` 已有骨架 | 已部分迁移 |
| `CommandExpandPattern` | 调试命令 | 1.20.1 未见命令迁移 | 缺失 |

## 已完成内容

### 1.20.1 已存在且可继续迭代的部分

- 现代 Forge 模组入口、注册和资源结构已经建立。
- 方块、物品、方块实体、菜单、创造标签均已注册。
- 现代菜单/界面工作流已经能承载样板编辑逻辑。
- AE2 现代 API 已接入 `PatternDetailsHelper.registerDecoder(...)`。
- 样板物品 tooltip、过滤模式、输入/输出列表、流体列表已有现代实现。
- JEI ghost ingredient 处理器已有基本实现。
- 当前 `migration/forge-1.20.1` 可以编译，说明工程脚手架阶段已经过了。

## 缺口清单

### A. 最关键：智能通配样板核心语义还没迁回来

旧版核心能力不是“记录几个输入输出槽”，而是：

- 利用 OreDictionary/通配规则把一个样板扩展为一组可工作的 AE2 样板。
- 支持像 `ingot* -> plate*` 这样的规则展开。
- 允许显式 ore 对、额外前缀、过滤模式共同参与生成结果。
- 在 AE2 侧拦截/过滤默认样板提供逻辑，避免重复和错误暴露。

当前 1.20.1 实现更接近：

- 把编辑器里看到的输入/输出直接编码到 NBT；
- 再由 AE2 decoder 把这份 NBT 直接解读为处理样板。

这意味着 1.20.1 现状更像“自定义处理样板编辑器”，还不是旧版那种“可按通配规则自动扩展”的智能样板系统。

### B. AE2 供样板链路未恢复

旧版 `TileEntityPatternExpander` 本身实现了 AE2 的 crafting provider，并负责：

- 扫描样板；
- 构造/缓存展开结果；
- 向 AE2 crafting tracker 提供可用样板；
- 配合接口侧注入逻辑过滤原始样板。

1.20.1 目前的 `PatternExpanderBlockEntity` 只处理：

- 物品存储；
- 菜单打开；
- capability 暴露；
- 方块破坏掉落。

也就是说，“编辑器方块存在”不等于“AE2 网络已经能识别这套智能样板逻辑”。

### C. OreDictionary 到现代 Tag/注册表语义的迁移未完成

旧版大量依赖：

- `OreDictionary.getOreNames()`
- `OreDictionary.getOres(...)`
- `OreDictionary.doesOreNameExist(...)`

现代版本没有相同语义的官方等价 API，因此必须明确设计替代方案：

1. 完全改成 item tags；
2. 自建“前缀 + 材料名”规则层；
3. 提供兼容旧 NBT 的解析层；
4. 允许配置文件声明显式映射对。

如果这层不先定下来，后面的 AE2 integration、GUI、tooltip 都会反复返工。

### D. 配置系统未迁移

旧版 `ModConfig` 里至少有这些现代线还没看到的东西：

- 扫描周期；
- 额外 ore 前缀；
- 显式 ore 对；
- 调试/行为相关配置。

现代版本如果准备长期维护，建议尽早用 `ForgeConfigSpec` 重建，不要把规则硬编码到 GUI 或 decoder 里。

### E. 气体兼容没有真正迁回来

旧版不只处理物品和流体，还处理了气体兼容，且用了较多反射去兼容第三方实现。

目前 1.20.1：

- 数据层主要看到了 items + fluids；
- 屏幕里还有 legacy gas 文本显示痕迹；
- 但没有看到完整的现代 gas 编码/解码/AE2 输出链路。

这部分建议在“通配规则”和“AE2 provider”稳定后再补，不建议现在就并行推进。

### F. 网络迁移还不完整

旧版有多种包：

- 设置槽位；
- 设置数量；
- 设置流体数量；
- 请求编码；
- 打开 GUI；
- 更新过滤规则；
- 更新模组过滤。

当前 1.20.1 只看到 `SetPatternSlotPacket`，说明现代菜单大概率在利用按钮/容器数据同步承担部分工作，但仍需要核对旧版所有交互是否已覆盖。

### G. 命令与调试能力未迁移

旧版存在 `CommandExpandPattern` 用于调试展开结果。

这类命令在迁移期价值很高，因为它能帮助你验证：

- 通配规则是否正确；
- NBT 编码是否正确；
- AE2 decoder 结果是否符合预期；
- 新旧版本行为是否一致。

建议在 1.20.1 尽早补一个 Brigadier 版本的调试命令。

### H. 模组身份与兼容策略需要尽快定案

目前：

- 旧版 `mod_id = sampleintegration`
- 1.20.1 `mod_id = techstart`

如果你的目标是“新版本延续旧模组”，那要尽快决定：

- 是否保留旧 `mod_id`；
- 旧 NBT key 是否原样兼容；
- 旧资源路径/翻译 key 是否尽量稳定；
- 玩家旧世界、脚本、整合包配置是否需要平滑升级。

如果这是“重命名后的新模组”，那也要同步写清迁移说明，避免未来自己也混乱。

## 推荐实施顺序

### 第一阶段：先抽 `common-core`

建议优先放入 `migration/common-core` 的内容：

- 样板槽位数据模型；
- 过滤规则模型；
- 通配规则展开服务；
- 文本/tooltip 载荷构建；
- NBT 键名与兼容策略常量；
- 排序、去重、校验逻辑。

不要先放进去的内容：

- `net.minecraft.*`
- Forge/NeoForge 事件总线；
- 菜单与界面；
- 网络通道实现；
- AE2 具体注册语句。

### 第二阶段：补 1.20.1 的核心行为

按这个顺序做最稳：

1. 设计现代“通配规则模型”；
2. 用 `common-core` 复写旧版 `OreDictRecipeCache` 的业务语义；
3. 让 `TechStartPatternDetails` 接入这套规则，而不是只读静态输入输出；
4. 为 `PatternExpanderBlockEntity` 补回 AE2 provider 侧能力；
5. 用调试命令和日志验证新旧行为一致。

### 第三阶段：再复制到 NeoForge 1.21.1

NeoForge 轨道应该复用：

- `common-core` 的规则；
- 1.20.1 验证过的行为契约；
- 尽量同一套 NBT/tooltip/filter 语义。

也就是说，NeoForge 不该先做玩法设计，而应当在 Forge 1.20.1 定义好行为后再移植加载器适配层。

## 下一批最值得直接开干的任务

### P0

- [ ] 在 `migration/common-core` 建立真正的 Java 源码目录和构建入口。
- [ ] 提取“样板输入/输出/流体/过滤模式”数据模型。
- [ ] 提取旧版 NBT key 常量与兼容读写策略。
- [ ] 设计旧版 OreDictionary 规则到现代 tags/映射表的替代方案。

### P1

- [ ] 为 1.20.1 重建配置系统。
- [ ] 为 1.20.1 增加调试命令。
- [ ] 为 1.20.1 补全其余编辑器交互同步。
- [ ] 明确 `sampleintegration` 与 `techstart` 的身份关系。

### P2

- [ ] 补气体兼容。
- [ ] 为 `common-core` 增加单元测试。
- [ ] 建立 1.12.2 / 1.20.1 行为对照样例集。
- [ ] 启动 NeoForge 1.21.1 代码轨道。

## 一个务实判断

如果你现在直接冲 NeoForge 1.21.1，会非常容易把“加载器 API 迁移”和“玩法逻辑重建”搅在一起。

更稳的路径是：

- 把 1.12.2 当作行为真值；
- 把 Forge 1.20.1 当作现代玩法参考线；
- 先把通配规则 + AE2 provider 这两个最大缺口补齐；
- 等 1.20.1 真正可玩后，再复制到 NeoForge。
