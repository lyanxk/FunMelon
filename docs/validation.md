# 验证记录

日期：2026-10-24。Minecraft 1.21.1，NeoForge 21.1.256，Java 21。

| 验证 | 结果 |
| --- | --- |
| `./gradlew.bat build` | 成功 |
| `./gradlew.bat runGameTestServer` | 25 个必需测试通过；未安装 Better Melon 时，扩展品种按条件不注册 |
| `./gradlew.bat runGameTestServer -PwithBetterMelon=true` | 25 个必需测试通过，包含五个品种的配方选择、种植、扩散、采收与掉落 |
| `./gradlew.bat runClientSmoke -PwithBetterMelon=true` | PASS：6 个品种、336 个方块状态、每品种 20 个果实模型选项、6 个种子模型；24 个有效贴图引用，其中 2 个动画贴图 |
| 附魔果皮动画 | 游戏贴图图集中实际载入 20 个 16×16 帧 |
| 美术校准 | Blockbench 对照原模组整瓜与瓜藤果实；10 张扩展品种果皮 PNG 的 SHA-256 与 JAR 来源一致 |
| 成品检查 | 无 Mixin、无 GameTest/客户端 smoke 类、未嵌入 Better Melon JAR、未覆盖 Better Melon 原配方 |

成品：`build/libs/funmelon-1.1.jar`，76,734 字节。

SHA-256：`74EAF48B4A3F521E87BA597B60AB8CF0229B88878472BDE72D571010A1B6747B`。

完整开发日志位于 `build/gametest-base-console.log`、`build/gametest-bettermelon-console.log`、`build/client-smoke-bettermelon-console.log`（构建目录，不提交）。测试为真实游戏进程中的自动验证；未通过 Computer Use 人工操作游戏。

## 上游诊断

用户提供的 Better Melon JAR 在加载其自身配方 `qa_better_melon:ender_carved_melon_torch_crafting` 时，报告引用不存在的 `qa_better_melon:deleted_mod_element`。该错误来自原 JAR 的独立雕纹瓜灯配方；本次未修改它，FunMelon 的制种与生长测试均通过。
## 开发客户端启动诊断与临时规避

2026-10-24 12:39 的普通开发客户端在初始化窗口时崩溃，报告为 `java.nio.file.FileSystemNotFoundException`，调用链经过 SecureJarHandler 3.0.8 的 `UnionFileSystemProvider.getFileSystem` 与 FML 4.0.45 的 `DisplayWindow.initRender`。该阶段尚未进入 FunMelon 的游戏逻辑。

检查本地依赖源码发现，UnionFileSystemProvider 的文件系统表使用普通 `HashMap`；创建和移除时加锁，但查询未加锁。独立调用该依赖的复现实验在后台持续查询一个始终存在的文件系统，同时在主线程创建其他文件系统，688,015 次查询中出现 16 次错误的 `FileSystemNotFoundException`；仅在复现实验中将表替换为 `ConcurrentHashMap` 后，19,413,809 次查询未出现该异常。结合崩溃日志中后台早期窗口渲染与主线程扫描 JAR 同时发生，这支持上游并发竞态的判断。实验文件保存在 `build/UnionRaceRepro.java`，不编入模组。

本机开发实例采用 [NeoForge 官方说明](https://docs.neoforged.net/user/docs/faq/#early-display-errors)中的临时规避：仅将 `run/config/fml.toml` 的 `earlyWindowControl` 改为 `false`。FML 源码中的该分支直接选择 `DummyProvider`，不启动早期窗口的后台 `initRender`。原配置备份为 `run/config/fml.toml.before-early-window-workaround-20261024.bak`；需要恢复时，将该设置改回 `true` 即可。此项是开发实例的 workaround，未修复上游依赖本身，也未修改模组代码、构建依赖或成品 JAR。修改后执行普通 `./gradlew.bat runClient`：已越过原崩溃位置，完成模组加载、声音引擎启动、全部贴图图集创建与资源重载；Minecraft NeoForge 1.21.1 窗口保持响应，未产生新崩溃报告。控制台记录为 `build/ide-client-recovery-console.log`。此验证通过 Gradle 启动，未通过 Computer Use 点击 IDE 的运行按钮；IDE client 使用同一 `run` 目录，配置会在下次启动时读取。

## 独立创造栏与默认 Better Melon

新增 `funmelon:funmelon` 创造栏，中文名称“趣味西瓜”，英文名称“FunMelon”。本地开发默认 `withBetterMelon=true`；上表早期不带参数的 GameTest 记录发生在切换默认值之前。现在验证独立运行需指定 `-PwithBetterMelon=false`。

- `./gradlew.bat build runClientSmoke` 成功：实际加载 Better Melon，创造栏精确包含 6 种种子且支持搜索；336 个方块状态、6 个种子模型及动画贴图检查通过。
- `./gradlew.bat runClientSmoke -PwithBetterMelon=false` 成功：创造栏仅包含普通种子，56 个方块状态与种子模型检查通过。
- 两次检查均在游戏客户端内实际构建创造栏，检查注册、标题翻译、图标、可见性、种子列表及搜索条目。日志为 `build/creative-tab-bettermelon-validation.log` 和 `build/creative-tab-standalone-validation.log`。
- 默认普通运行执行 `./gradlew.bat neoForgeIdeSync runClient`：日志确认加载 Better Melon 26.10.1，窗口正常响应；随后记录到玩家进入单人世界并获得 Better Melon 进度。日志为 `build/creative-tab-normal-client.log`。成品 JAR 检查未包含测试类、嵌套 JAR 或 Mixin 配置。

## 1.1 瓜藤颜色

五种扩展瓜藤在 Blockbench 制作固定彩色叶纹理：黄玉黄、未熟嫩青绿、闪烁金橙、附魔青蓝、末地紫粉。普通瓜藤继续采用群系草色。幼藤与成熟瓜藤均使用各自叶纹理；扩展叶面的 `tintindex` 和扩展草色回调已移除。

最终执行 `./gradlew.bat build runClientSmoke` 成功，日志为 `build/v1.1-final-client-validation.log`。实际检查六品种共 336 状态、每品种 20 种果实姿态、六种创造栏种子、全部模型贴图，以及普通叶 tint=0 / 扩展叶无 tint。交换附魔和末地叶色后重新验证通过。最终署名更新后的打包亦通过，日志为 `build/v1.1-final-package.log`。

像素检查：五张扩展叶 PNG 都为 16×16、225 个不透明像素、四级明暗，透明通道与普通叶底图完全一致。果皮 PNG 与动画元数据哈希均未变化。预览与可编辑源为 `F:miscBlockBenchunmelonine_colors_1_1_preview.png` / `.bbmodel`；上排幼藤、下排成熟，左至右普通、黄玉、未熟、闪烁、附魔、末地。