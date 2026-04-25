# Common Core Boundary

`common-core` 不是可运行模组，而是给 Forge 1.20.1 与 NeoForge 1.21.1 共用的纯 Java 逻辑层。
`common-core` is not a runnable mod; it is a pure Java logic layer shared by the Forge 1.20.1 and NeoForge 1.21.1 tracks.

## Put Here

- 样板编码/解码的业务规则。
- 输入/输出/流体/气体的数据模型转换。
- 黑白名单过滤语义。
- 不依赖客户端 API 的文本载荷构建。
- 排序、校验、去重等确定性工具。
- 通配规则、前缀规则、显式映射规则。

## Do Not Put Here

- 注册事件逻辑。
- 物品、方块、菜单、界面类。
- Forge/NeoForge 事件总线代码。
- 网络通道实现细节。
- 任何直接导入 `net.minecraft`、Forge、NeoForge 的类。

## Package Layout

- `com.wuxiaoya.techstart.core.model`
- `com.wuxiaoya.techstart.core.codec`
- `com.wuxiaoya.techstart.core.codec.tag`
- `com.wuxiaoya.techstart.core.service`
- `com.wuxiaoya.techstart.core.validation`

## Current Bootstrap

本轮已落地的最小骨架：

- `PatternDefinition`：样板聚合模型。
- `PatternEntry`：输入/输出/流体/气体统一条目模型。
- `FilterMode` / `FilterEntry`：过滤模式与条目序列化规则。
- `PatternNbtKeys`：新旧版本共用的关键 NBT 键常量。
- `PatternTooltipPayloadBuilder`：不依赖 Minecraft 客户端 API 的 tooltip 文本载荷构建。
- `PatternValidators`：最基础的结构校验。
- `WildcardRuleConfig`：通配规则配置。
- `WildcardRecipeResolver`：通配展开内核。
- `WildcardRecipeFilter`：对展开结果应用 blacklist / whitelist 过滤。

## Readers

当前已经补上无 Minecraft 依赖的读取接口：

- `TagObject`：抽象读取接口。
- `MapTagObject`：基于 `Map<String, ?>` 的默认实现，方便未来接单元测试和适配层。
- `LegacyPatternDefinitionReader`：把 1.12.2 样板 NBT 读取成 `PatternDefinition`。
- `ModernPatternDefinitionReader`：把 1.20.1 样板 NBT 读取成 `PatternDefinition`。

## Wildcard Rules

当前通配规则层已支持：

- 默认前缀：`ingot` / `plate` / `block` / `nugget` / `rod` / `gear` / `wire` / `dust`
- 追加自定义前缀
- 追加自定义 ore/tag 名称
- 显式映射对，例如 `ingotCopper -> plateCopper`
- 从 `ingot* -> plate*` 这类通配表达式中解析前缀并提取材料名
- 对展开结果应用过滤模式与过滤条目
- 兼容 `pair` 与 `pair|variant` 两类过滤 ID 语义

这层目前仍是“纯字符串规则”，还没有和真实注册表、标签系统绑定；这是刻意的，目的是让 Forge / NeoForge 侧只负责把可用 key 列表提供进来。

## Next Steps

1. 增加 Forge / NeoForge 侧的薄适配器，把真实 `CompoundTag` 接到 `TagObject`。
2. 增加旧版 `NBTTagCompound` 到 `TagObject` 的适配器或导出器。
3. 把现代标签/旧 OreDictionary 收集结果转换为 `WildcardRecipeResolver` 所需的 key 列表。
4. 让 1.20.1 侧实际调用 `common-core` 的读取器、展开器、过滤器。
5. 为 `common-core` 增加单元测试和样例数据。
