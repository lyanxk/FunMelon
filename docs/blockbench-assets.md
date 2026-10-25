# Blockbench 资源记录

制作日期：2026-10-24。使用 Blockbench 5.2.2 MCP，未使用 Computer Use。

## 可编辑文件

位于 `F:\misc\BlockBench\funmelon\`：

- `watermelon_leaves.bbmodel`
- `watermelon_stand.bbmodel`
- `watermelon_lay.bbmodel`
- `watermelon_rotated1.bbmodel`
- `watermelon_rotated2.bbmodel`
- `watermelon_rotated3.bbmodel`
- `melon_seeds.bbmodel`
- `mature_melon_preview.bbmodel`：将瓜藤与成熟果实组合的检查场景，不作为游戏模型导出。
- `mature_melon_preview.png`：Blockbench 原生视口截图。
- `reference/`：上游固定提交的参考模型及方块状态。
- `textures/`：参考贴图与在 Blockbench 中绘制的独立种子图标。

全部模型项目采用 `java_block` 格式，目标格式选择 `1.9 - 1.21.5`，适用于本项目 Minecraft 1.21.1。种子使用 `minecraft:item/generated` 二维物品模型，不涉及三维物品的镜像显示约定。

## 游戏资源

导出目录为 `src/main/resources/assets/funmelon/`：

- 六个 `models/block/watermelon_*.json`。
- 三张 `textures/block/watermelon_*.png`，均为 16×16。
- `blockstates/melon_vine.json`：始终显示瓜藤，年龄 7 时在五种姿态和四个方向中选取果实外观。
- `models/item/melon_seeds.json` 与 `textures/item/melon_seeds.png`：独立种子图标，16×16，在 Blockbench 内绘制。

瓜藤和果实通过 Blockbench 原生 Java 模型解析器导入，加载贴图，调整命名空间后由原生 Java codec 编译导出；`.bbmodel` 使用原生项目 codec 保存，包含可编辑元素与纹理。

普通品种保留原始几何和 UV、`cutout` 渲染、灰度叶片及 `tintindex=0`，游戏内由客户端方块颜色注册提供群系草色。扩展品种采用下文所列固定叶藤颜色，便于在未结果时区分。

来源为 Tea the Story : Recount 提交 `cf0ffb99aee08a139d4272c3776d3ab9161fb7e1`，资源许可证与作者记录位于 `src/main/resources/licenses/funmelon-art.txt`。果实模型保留了原作者 MalayP 的 credit。

## 验证

已在 Blockbench 检查成熟瓜藤组合的几何与纹理，并检查独立种子图标；导出模型的贴图引用已改为 `funmelon`，未引入实体模型、基岩版格式或自定义渲染器。本记录不代表游戏客户端实测通过。

## Better Melon 五种品种

新增 `yellow`、`unripe`、`glistering`、`enchanted`、`ender`。每种均有独立种子图标、五个果实姿态模型、叶藤模型、三张方块贴图和 multipart blockstate；各自保留同名 `.bbmodel` 源文件。模型在 Blockbench 中导入、调整材质与 UV，再使用原生 Java codec 导出。

果皮严格参照用户提供的 `qa_better_melon-26.10.1-neoforge-1.21.1.jar` 中对应 `{prefix}_melon_block.json`，保留原始像素，不人为改变品种配色。黄瓤与未熟西瓜在原模组中使用相同绿色果皮，保持这一设计。面映射为上下使用 top，其余四面使用 side；将完整 16×16 面映射到茶风果实姿态，避免使用 12×12 UV 裁掉原贴图条纹。果实大小与摆放沿用茶风的五个姿态和四向随机。

| 品种 | Better Melon side / top 文件 |
| --- | --- |
| yellow、unripe | `melon_side.png` / `melon_top.png` |
| glistering | `melon_side_glistering.png` / `melon_top_glistering.png` |
| enchanted | `enchanted_melon_side.png` / `enchanted_melon_top.png` |
| ender | `melon_side_ender.png` / `melon_top_ender.png` |

10 张导出果皮 PNG 与来源文件逐个 SHA-256 比对一致。附魔西瓜保留原始 16×320、20 帧贴图及原始 `.mcmeta`（frametime=4，不插值）。五种种子是 Blockbench 内绘制的独立二维图标。

原始整块参考 JSON/PNG 及 Blockbench 整块参考模型位于 `reference/bettermelon/`。`source_vs_funmelon_preview.png` 为此前的果实对照图：左侧原模组完整方块，右侧瓜藤上的果实；其中旧版叶藤颜色已由下述 1.1 预览取代，果皮仍以原始 Better Melon 素材为准。

## 1.1 叶藤品种配色

按用户要求，五种扩展叶藤在 Blockbench 原生纹理编辑中，使用茶风灰度叶图的四级明暗比例染色；保留 16×16 尺寸、225 个不透明像素、原透明轮廓、几何和 UV。扩展叶模型移除全部 `tintindex`，避免固定颜色再被群系草色覆盖；普通品种保持群系绿色。所有年龄共用各自叶模型，因此未成熟与成熟瓜藤均可按颜色辨认。

| 品种 | 叶藤颜色 | 最亮像素 |
| --- | --- | --- |
| 普通 | 群系草绿色 | 原灰度底图，由游戏染色 |
| yellow | 黄玉黄 | `#F6DC4F` |
| unripe | 浅嫩青绿 | `#A6E5B9` |
| glistering | 金橙 | `#FF9A28` |
| enchanted | 青蓝 | `#39C7D2` |
| ender | 紫粉 | `#D470F4` |

附魔与末地叶藤的最终配色已按用户后续要求交换；各品种 ID、果皮、果肉参考与动画不互换。五个 `{prefix}_watermelon_leaves.bbmodel`、`textures/{prefix}_watermelon_leaves.png` 和 `{prefix}_mature_melon.bbmodel` 均同步更新，游戏使用的 PNG 与 Java JSON 由 Blockbench 导出到原资源路径。原 Java 模型署名保留。

`F:\misc\BlockBench\funmelon\vine_colors_1_1_preview.bbmodel` 是六品种并排检查场景，配套 `vine_colors_1_1_preview.png` 为 Blockbench 原生视口截图。上排未结果、下排成熟；从左至右为普通、黄瓤、未熟、闪烁、附魔、末地。预览场景仅为普通叶藤使用代表性草色，普通游戏贴图仍为灰度。该场景不作为游戏模型导出。

资源核验：五份叶模型除去 `tintindex` 外与改色前一致；五张叶 PNG 透明通道不变，各保留四级颜色；所有果皮及附魔动画元数据哈希未变。客户端验证另见验证记录。

Better Melon 贴图作者为 LitQA，JAR 元数据声明 `CC-BY-NC 4.0`；茶风几何和叶藤素材继续保留其 `CC-BY-NC-SA 4.0` 来源记录。两者在资源声明中分别列明，不将 Better Melon 原图错误标记为茶风许可证。
