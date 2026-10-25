# 项目协作说明

本文件适用于 FunMelon 项目及其子目录。

## 项目基础

- 本项目是 Minecraft Java 版 NeoForge 模组，模组 ID 为 `funmelon`。
- 本模组用于用户自己的服务器；源码仓库可以公开，不创建 GitHub Release。
- 当前目标为 Minecraft `1.21.1`，使用 Java `21`；具体依赖和版本以 `gradle.properties`、`build.gradle` 为准。
- Java 源码位于 `src/main/java/org/lyy/funmelon/`。
- 客户端资源位于 `src/main/resources/assets/funmelon/`。
- 数据生成输出位于 `src/generated/resources/`，模组元数据模板位于 `src/main/templates/`。

## 模型与材质

- 需要制作或修改模型、材质、贴图时，使用项目挂载目录 `F:\misc\BlockBench` 下的 Blockbench 制作。
- 仅制作适用于 Minecraft Java 版的模型。根据用途选择 Java 版方块／物品模型或 Java 版实体模型格式，不制作或导出基岩版模型。
- Blockbench 可编辑源文件（如 `.bbmodel`）及配套制作素材保存在 `F:\misc\BlockBench` 下，按项目和资源名称组织，避免覆盖无关文件。
- 将游戏实际使用的模型、贴图等导出到本项目对应资源目录；Java 实体模型源码放入对应 Java 包，保持资源路径与代码引用一致。
- 导出后检查模型格式、贴图引用和命名空间，确保与本项目目标版本及渲染实现兼容。

## 开发与验证

- 不使用 Mixin。新西瓜种子独立于原版种子，由一粒原版西瓜种子以 1:1 无序合成获得。
- 瓜藤行为优先沿用茶风·纪事：追叙的实现，保留来源记录；支持空手右键采收。
- Better Melon 兼容包括各品种西瓜片合成对应独立种子、各自瓜藤和果实模型材质，以及采收对应 Better Melon 整瓜；普通品种采收原版西瓜。避免拦截 Better Melon 对整瓜的交互。
- 具体代码实现与测试子代理使用 GPT-6.1-Sol、high 推理强度；美术和架构设计使用 Astra。
- 遵循现有代码结构与命名方式，保留用户已有修改。
- 使用项目自带的 Gradle Wrapper；Windows 下构建命令为 `./gradlew.bat build`，启动开发客户端为 `./gradlew.bat runClient`。
- 根据修改范围执行必要验证；仅修改说明文档时无需启动游戏或运行完整构建。

## 终端与操作授权

- 优先使用 PowerShell 7（`pwsh`），确保终端输入输出和文本文件读写使用 UTF-8，避免中文乱码。
- 使用 Computer Use 前必须先取得用户许可；用户已明确要求使用 Computer Use 时，无需重复询问。此要求同样适用于通过 Computer Use 操作 Blockbench。
