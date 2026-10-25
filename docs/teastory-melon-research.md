# 茶风·纪事：追叙 — 西瓜实现调研

调研日期：2026-10-24。目标是为 FunMelon 私人服务器模组复刻同款西瓜提供依据。本次仅检查公开源码与文档，未运行原模组，也尚未实现游戏功能。

## 核对版本

- [作者发布页](https://www.curseforge.com/minecraft/mc-mods/tea-the-story-recount)
- [公开仓库](https://github.com/TeamTeaMC-Misc/Tea-the-Story)
- 分支：`neoforge-1.21.x`。
- 固定提交：`cf0ffb99aee08a139d4272c3776d3ab9161fb7e1`。
- 该提交的 `gradle.properties`：Minecraft 1.21.1、NeoForge 21.1.129、模组版本 0.4.6.0.0.1。与 FunMelon 的 Minecraft 目标一致；FunMelon 使用更新的 NeoForge 21.1.256。
- [官方游玩说明](https://tea-the-story.readthedocs.io/en/latest/zh/play/#_8)可以辅助理解；具体行为以此提交源码为准。

## 玩法与状态

| 项目 | 已核对的原实现 |
| --- | --- |
| 种植 | 原版西瓜种子右键种植，目标格为空气、下方为耕地时，改放自定义瓜藤；非创造模式消耗一粒种子。 |
| 状态 | 一个 `MelonVineBlock` 方块，保存 `age=0..7` 和 `distance=1..7`。默认年龄 0、距离 1。 |
| 自然生长 | 服务端随机刻驱动，要求附近区域已加载、亮度至少 9；水平四邻有成熟瓜藤时暂停。成功生长时年龄加 1。 |
| 扩散 | 本次生长后的年龄小于 7 且距离小于 7 时，随机选一个水平方向；相邻格为空气且下方可支撑，就放置年龄 0、距离加 1 的瓜藤。成熟的那次生长不扩散。 |
| 支撑面 | 泥土标签内的方块、耕地或满足刚性支撑检查的方块。因此扩散不只发生在耕地上。 |
| 生长概率 | 成功概率为 `1 / (floor(25 / f) + 1)`；`f` 来自下方 3×3 土地的可种植性、肥沃程度及瓜藤密植情况，通过 NeoForge 作物生长事件钩子处理。 |
| 成熟 | 年龄 7 时在瓜藤上叠加西瓜外观，没有额外生成一个西瓜方块或实体。 |
| 剪刀采收 | 成熟时用剪刀右键：损耗 1 耐久、掉落一个原版整颗西瓜、播放剪刀声，瓜藤重置为默认状态。 |
| 破坏掉落 | 未成熟掉种子；成熟掉整颗西瓜并额外掉种子，种子数量受时运加成和爆炸衰减规则影响。 |
| 骨粉 | 仅未成熟且下方为耕地时成功，年龄增加随机 2～5、最高到 7；这一过程不触发扩散。 |
| 踩踏保护 | 上方有瓜藤的耕地取消踩踏事件。 |
| 切瓜 | 剑右键原版西瓜方块会移除它、消耗 1 耐久，并按该西瓜的掉落表产出物品。不是额外的切片模型。 |
| 开关 | `Agriculture.BetterMelon` 默认启用，只控制种植替换入口；切瓜与踩踏保护的注册不受该开关控制。 |

核心源码：

- [MelonVineBlock.java](https://github.com/TeamTeaMC-Misc/Tea-the-Story/blob/cf0ffb99aee08a139d4272c3776d3ab9161fb7e1/src/main/java/com/teamtea/teastory/block/crops/MelonVineBlock.java)
- [AgricultureEventHandler.java](https://github.com/TeamTeaMC-Misc/Tea-the-Story/blob/cf0ffb99aee08a139d4272c3776d3ab9161fb7e1/src/main/java/com/teamtea/teastory/handler/event/AgricultureEventHandler.java)
- [CommonEventHandler.java](https://github.com/TeamTeaMC-Misc/Tea-the-Story/blob/cf0ffb99aee08a139d4272c3776d3ab9161fb7e1/src/main/java/com/teamtea/teastory/handler/CommonEventHandler.java)
- [掉落表](https://github.com/TeamTeaMC-Misc/Tea-the-Story/blob/cf0ffb99aee08a139d4272c3776d3ab9161fb7e1/src/generated/resources/data/teastory/loot_table/blocks/watermelon_vine.json)

## 外观实现

- 使用 Java 版普通 JSON 方块模型和 `multipart` 方块状态，不需要实体模型或方块实体渲染器。
- 所有年龄始终显示同一套 `watermelon_leaves`；只有年龄 7 叠加果实，因此没有逐阶段变大的瓜体。
- 果实有 `stand`、`lay`、`rotated1`、`rotated2`、`rotated3` 五种模型，每种配合 0/90/180/270 度旋转，共 20 个可选外观。
- 果实本体为 12×12×12 模型单位的立方体，依靠贴图方向、偏移和旋转产生直立、横躺、斜放效果。三个斜放模型分别采用 -22.5、-45、22.5 度的 Y 轴旋转。
- 瓜藤采用带透明镂空的低矮模型；叶片带 `tintindex=0`，客户端按生物群系草色着色。
- 共三张贴图：`watermelon_leaves.png`、`watermelon_side.png`、`watermelon_top.png`。
- 选择框未成熟为 16×5×16、成熟为 16×13×16；注册时启用 `noCollission`，不能把选择框当实体碰撞箱。
- 模型文件中保留了 Blockbench 制作信息。复刻时按本项目约定，在 `F:\misc\BlockBench` 下使用 Blockbench 制作并保留 `.bbmodel`，导出 Java 版资源。

资源和颜色入口：

- [方块状态及 20 种变化](https://github.com/TeamTeaMC-Misc/Tea-the-Story/blob/cf0ffb99aee08a139d4272c3776d3ab9161fb7e1/src/main/resources/assets/teastory/blockstates/watermelon_vine.json)
- [直立模型](https://github.com/TeamTeaMC-Misc/Tea-the-Story/blob/cf0ffb99aee08a139d4272c3776d3ab9161fb7e1/src/main/resources/assets/teastory/models/block/watermelon_stand.json)
- [叶片模型](https://github.com/TeamTeaMC-Misc/Tea-the-Story/blob/cf0ffb99aee08a139d4272c3776d3ab9161fb7e1/src/main/resources/assets/teastory/models/block/watermelon_leaves.json)
- [颜色注册](https://github.com/TeamTeaMC-Misc/Tea-the-Story/blob/cf0ffb99aee08a139d4272c3776d3ab9161fb7e1/src/main/java/com/teamtea/teastory/client/ClientSetup.java)

## 需要明确保留或修正的细节

1. `distance` 是扩散时记录的代数，并不动态追踪根部或计算到根部的真实距离；移除初始藤蔓不会因此自动清除其他藤蔓。
2. 剪刀采收使用 `defaultBlockState()`，所以年龄和距离都会重置。连续采收可以重新获得扩散能力，不能把距离 7 理解成永久的瓜田半径上限。
3. 骨粉不检查自然生长所用的邻瓜限制、亮度或扩散条件，可以催熟相邻瓜藤。
4. `isValidBonemealTarget()` 始终返回 true，成功条件却更严格；复刻时需实测无效目标上的骨粉消耗，不能宣称只有成功才消耗。
5. 官方说明写支撑移除后掉落。瓜藤类未定义独立下落实体逻辑，而是依赖 `BushBlock` 的支撑更新机制和掉落表；不能把它解释成会滚动的西瓜或重力物理动画。
6. 原种植及采收代码包含客户端世界状态修改。FunMelon 实现时应由服务端决定物品扣除、掉落及方块变更，并验证双手交互、创造模式、多人同步和区块重载。

## FunMelon 实现范围建议

以该 1.21.1 版本为行为基准，实现瓜藤方块、种植入口、剪刀采收、剑切瓜、掉落表、踩踏保护及客户端草色；资源采用一套瓜藤模型、五套果实姿态和三张贴图。逻辑与资源均使用 `funmelon` 命名空间，产物继续使用原版西瓜和种子。无需引入茶叶、水稻、灌溉等其他系统。

验证至少覆盖：从种植到自然成熟、扩散边界、邻瓜抑制、骨粉限制、剪刀再生、破坏与失去支撑掉落、剑切瓜、耕地保护、多人交互和保存重载。客户端和服务器都需要安装该模组，客户端需注册自定义方块及加载资源。

## 来源记录

[仓库 README](https://github.com/TeamTeaMC-Misc/Tea-the-Story/blob/cf0ffb99aee08a139d4272c3776d3ab9161fb7e1/README.md)分别标明代码为 BSD-3、资源为 CC BY-NC-SA 4.0。后续如直接移植代码或使用原资源，应随相关文件保留对应来源、作者和许可信息；本项目目前仅记录调研结论，尚未引入其代码或资源。
