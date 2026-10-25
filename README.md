# FunMelon

Minecraft Java 1.21.1 / NeoForge 21.1.256 模组，用于私人服务器。提供独立种子和茶风·纪事：追叙式地面瓜藤，不使用 Mixin，不修改原版西瓜种子的种植行为。

## 制种

以下新配方均为无序合成，每次产出一粒 FunMelon 种子。

| 品种 | 合成材料 | 采收产物 |
| --- | --- | --- |
| 普通 | 1 粒原版西瓜种子 | 原版西瓜 |
| 黄玉 | 1 片黄玉西瓜片 + 1 粒原版西瓜种子 | Better Melon 黄玉西瓜 |
| 未熟 | 1 片未熟西瓜片 | Better Melon 未熟西瓜 |
| 闪烁 | 1 片原版闪烁的西瓜片 | Better Melon 闪烁的西瓜 |
| 附魔 | 1 片附魔西瓜片 | Better Melon 附魔西瓜 |
| 末地 | 1 粒 Better Melon 末地西瓜种子 | Better Melon 末地西瓜 |

后五个品种仅在安装 `qa_better_melon` 时注册。保留 Better Melon 原有配方：末地西瓜片先合成它自己的末地种子，再转换成瓜藤种子；黄玉西瓜片原本合成普通种子的配方也保留。未安装 Better Melon 时，普通瓜藤可独立使用。

创造模式物品栏提供独立的“趣味西瓜 / FunMelon”分类，以普通瓜藤种子为图标；包含普通种子和已加载的 Better Melon 五个品种种子。未安装 Better Melon 时仅显示普通种子。

## 种植和采收

- 在耕地种下 FunMelon 种子。瓜藤沿地面随机扩散，成熟时显示对应西瓜。
- 主手空手右键采收，或使用剪刀右键采收；采收保留瓜藤，重新开始生长。
- 产出各品种的原版或 Better Melon 整瓜，可继续使用 Better Melon 自己的切割、雕刻和食用功能。
- 骨粉只能成功催熟耕地上未成熟的瓜藤；为保持茶风行为，对无效目标使用仍可能消耗骨粉。
- 瓜藤下方耕地具有踩踏保护。破坏未成熟瓜藤掉落对应种子，成熟时掉整瓜和对应种子。
- 沿用茶风的年龄与扩散距离规则，包括剪刀或空手采收时将年龄与距离一起重置。
- 普通藤叶随群系草色变化；扩展品种使用固定叶色便于辨认：黄玉为黄色、未熟为嫩青绿、闪烁为金橙、附魔为青蓝、末地为紫粉。

普通种子 ID：`funmelon:melon_seeds`。扩展种子 ID：`funmelon:{yellow,unripe,glistering,enchanted,ender}_melon_seeds`。对应瓜藤将 `_seeds` 换成 `_vine`。

## 安装与开发验证

客户端和服务器都安装 `build/libs/funmelon-1.1.jar`。使用扩展品种时，两端另装 Better Melon；其 JAR 不会打包进 FunMelon。

本项目验证使用用户提供的 `qa_better_melon-26.10.1-neoforge-1.21.1.jar`。其 SHA-256 为 `36459680C7700D9C44B3707D639D1E8C0F287518C133FB1AF17C017AC7BBD09A`。

仓库不包含 Better Melon 原始 JAR。克隆后，请将上述版本的 JAR 放到项目根目录；如果只开发普通瓜藤，可在 Gradle 命令中添加 `-PwithBetterMelon=false`。源码公开存放，不提供 GitHub Release；使用 Java 21 和项目自带的 Gradle Wrapper 构建。

`gradle.properties` 默认设置 `withBetterMelon=true`，IDE 同步和开发运行均加载项目根目录中的上述 JAR。使用 `-PwithBetterMelon=false` 可验证未安装 Better Melon 的运行方式。此依赖仅用于开发运行，不会嵌入成品 JAR。

```powershell
./gradlew.bat build
./gradlew.bat runGameTestServer
./gradlew.bat runGameTestServer -PwithBetterMelon=false
./gradlew.bat runClientSmoke
./gradlew.bat runClientSmoke -PwithBetterMelon=false
./gradlew.bat runClient
```

GameTests 位于 `src/gameTest/`，在真实 Minecraft 服务端验证种植、合成、扩散、采收和兼容行为。客户端 smoke 检查加载后的模型、贴图引用、普通与扩展藤叶的染色设置及独立创造模式分类中的种子，并自动退出。这些验证代码不会进入成品 JAR。

## 模型与来源

Blockbench 可编辑源文件及预览保存在 `F:\misc\BlockBench\funmelon`。所有游戏模型均为 Java 版 JSON 方块或物品模型。瓜藤结构与摆放方式参考茶风；扩展品种的果实纹理和面映射以 Better Melon 原方块为依据。

茶风原实现调研见 [docs/teastory-melon-research.md](docs/teastory-melon-research.md)。改编代码保留 BSD-3-Clause 来源说明，茶风资源按 CC BY-NC-SA 4.0 记录，Better Melon 纹理按其 JAR 声明的 CC BY-NC 4.0 记录；具体说明随资源保留于 `META-INF/licenses/` 和 `licenses/`。
