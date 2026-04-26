# 贡献指南

## 开发环境

- Java 21+
- Node.js 18+
- pnpm

## 开发

```bash
# 构建插件
./gradlew build

# 开发前端
cd ui
pnpm install
pnpm dev
```

## 构建

```bash
./gradlew build
```

## GitHub 自动化

- `.github/workflows/ci.yaml`
  - PR 合并前必须通过必需检查 `CI / Required Checks`。
  - 此工作流负责 PR 的主要检查，包括插件构建、测试，以及发版版本号约束。
  - 普通 PR 可以照常改代码，但不能修改 `gradle.properties` 里的 `version`；如果修改了，这个工作流会直接失败。
  - 只有带 `release` 标签的 PR 才允许修改 `gradle.properties` 里的 `version`，且版本号必须是递增的语义化版本；不满足时同样由这个工作流拦截。
- `.github/workflows/check-release-guard.yml`
  - 为带 `release` 标签的 PR 提供额外的发布摘要检查。
  - 会在 PR 中标记上一个稳定版标签和本次请求发布的版本号，便于合并前复核。
- `.github/workflows/release-stable-plugin.yml`
  - 负责带 `release` 标签的 PR 合并后的正式发版流程。
  - 会校验合并后的 `main` 是否仍然对应这次发版 PR、把 `CHANGELOG.md` 的 `Unreleased` 提升为正式版本、提交更新日志变更，并创建 GitHub 发布页（Release）。
  - 随后会自动把同一批构建产物同步到 Halo 应用市场。

## 发布流程

### 发布前检查清单

1. 准备合并到本次版本的提交和 PR 已全部进入 `main` 之前的待发布 PR。
2. `CHANGELOG.md` 的 `## [Unreleased]` 下已经补充完整本次版本说明，并且没有删除 `## [Unreleased]` 标题。
3. 发布 PR（带 `release` 标签）只允许手动修改 `gradle.properties` 中的 `version`，该值将作为正式版目标版本号。
4. 发版前人工确认 `src/main/resources/plugin.yaml` 的 `spec.requires` 仍符合目标 Halo CMS 版本要求。

### 正式版发布方法

正式版通过带标签的 PR 自动发布：

1. 创建用于正式发布的 PR。
2. 为 PR 添加 `release` 标签，并将 `gradle.properties` 中的 `version` 改为目标语义化版本号，例如 `3.1.2`。
3. 等待 `CI / Required Checks` 与 `Check Release Guard / Check Release Constraints` 通过，并确认摘要中的目标版本号与上一个稳定版无误。
4. 合并 PR 到 `main`。

PR 合并后，机器人会自动执行以下动作：

1. 将 `CHANGELOG.md` 中 `## [Unreleased]` 的内容提升为本次正式版条目，并保留 `## [Unreleased]` 标题。
2. 自动重建 `CHANGELOG.md` 末尾的版本对比链接定义。
3. 将更新日志变更提交回 `main`。
4. 构建全部发布变体，并创建 GitHub Release。
5. 同步同一批构建产物到 Halo 应用市场。

### 发布产物顺序

GitHub Release 页面和 Halo 应用市场最终展示顺序都固定为：

1. `lite`
2. `linux-x86_64`
3. `linux-arm64`
4. `macos-arm64`
5. `macos-x86_64`
6. `windows-x86_64`
7. `all-platforms`

注意：

- GitHub Release 侧按上述顺序直接上传附件。
- Halo 应用市场侧会按展示逆序处理上传顺序，因此自动化脚本会反向上传，以保证最终展示顺序仍与上面的列表一致。

## 配置约定

- 默认值设定由 `src/main/resources/extensions/settings.yaml` 统一定义。
- 配置类、`fallbackConfig()`、配置提供方（Supplier）和运行时初始化逻辑中，不要重复写同一份业务默认值。
- 设置缺失、读取失败或对象为空时，可以返回最小可用的兜底配置；但兜底配置不应再维护一份和 `settings.yaml` 并行的业务默认值。
- 如果某个配置项在运行时可能出现非法值，并且会导致初始化失败，或者旧数据、脏数据可能绕过界面约束，应在消费前校验，必要时修正为可用值，而不是在配置类中再补一套默认值。

### 资源处理任务

- __processUiResources__ - 处理UI前端资源，将ui子项目的构建输出复制到console目录
- __processShikiResources__ - 处理Shiki代码高亮的JS资源，仅完整版需要
- __processLiteResources__ - 专门为轻量版处理资源，排除JS相关内容

### 📦 核心构建任务

- __jarLite__ - 构建轻量版，完全排除JS功能和Javet依赖
- __jarFullAllPlatforms__ - 构建包含所有平台支持的完整版
- __jarFull{Platform}__ - 构建特定平台的完整版(如jarFullLinux-x86_64)

### 🚀 便捷任务

- __buildAll__ - 构建所有版本
- __buildLite__ - 仅构建轻量版
- __build__/__jar__ - 默认构建包含所有平台支持的完整版

构建完成后，可以在 `build/libs` 目录找到插件 jar 文件。

## 已知问题

以下问题属于当前架构和上游依赖形态带来的限制，开发时需要明确认知。

### HTML 页面压缩的响应体改写不是流式的

- 相关实现：
  - `src/main/java/top/howiehz/halo/plugin/extra/api/service/interop/web/filter/htmlminify/HtmlMinifyWebFilter.java`
- 当前 HTML 页面压缩功能依赖 Halo 的附加 Web 过滤器扩展点（`AdditionalWebFilter`）完整读取并重写响应体。
- 这意味着在压缩前必须先聚合完整的 HTML 响应内容，再交给 `minify-html` 处理。
- 因此该功能天然会带来一次额外的内存占用和复制成本，无法像真正的流式转换那样边读边压。
- 这不是当前实现的疏漏，而是 Halo 附加 Web 过滤器扩展点（`AdditionalWebFilter`）的接入方式和 `minify-html` 接口形态共同决定的限制。

## 修订发布说明

请在 `CHANGELOG.md` 的 `## [Unreleased]` 下记录本次正式版需要对外说明的变更。

## 如何添加新的嵌入式 JS 模块

本项目将 JavaScript 工具嵌入到 Java 运行时中，并将其预加载到 Javet V8 运行时中。按照以下步骤添加对新 JS 模块的支持。

### 在 `JsModule` 枚举中添加条目

- 文件：`src/main/java/top/howiehz/halo/plugin/extra/api/service/interop/runtime/module/JsModule.java`
- 为模块添加一个枚举常量。UMD 模块 `marked` 的示例：

```java
MARKED("marked", "marked.umd.js", JsModuleType.UMD),
```

- 该枚举将 `module.getModuleName()` 映射到 `js/<name>`。`getSourceCode()` 将加载 `resources/js/<fileName>`。

### 将 JS 文件放在 resources 目录下

- 路径：`src/main/resources/js/`
- 名称必须与您添加的 `fileName` 匹配。示例：`marked.umd.js`。
- 该文件可以是真实的库构建文件或精简的 UMD 文件。对于 ESM 或其他模块类型，请相应调整 `JsModuleType`。

### 预加载模块（可选但推荐）

- 文件：`src/main/java/top/howiehz/halo/plugin/extra/api/service/interop/runtime/engine/CustomJavetEngine.java`
- 引擎当前在 `preloadModules()` 中预加载 `Shiki`
- 使用 `JsModule.MARKED.getSourceCode()` 读取资源，使用 `v8Runtime.getExecutor(code).executeVoid()` 执行。
- 加载后，验证预期的函数是否暴露在 `globalThis` 这类全局入口上。保持预加载对错误的容忍性，避免引擎创建失败。

### 暴露 Java 服务来调用模块

- 在 `service/interop/runtime/adapters/<module>` 下创建服务接口（示例 `service/interop/runtime/adapters/marked/MarkedService.java`），定义您需要的操作。
- 使用 Spring `@Service` 类实现接口，该类使用现有的 `V8EnginePoolService` 对运行时执行调用，类似于 `ShikiHighlightServiceImpl`。
- 优先读取 `globalThis` 上的方法（例如 `parseMarkdown`）或 `globalThis.<lib>.parse`。

### 验证

- 使用 `V8EnginePoolService.executeScript` 或 `withEngine` 调用函数并验证结果。
- 在 `CustomJavetEngine.preloadModules()` 中添加快速布尔检查，如 `typeof parseMarkdown === 'function'` 并记录结果。

### 模块类型和自定义解析器

- JsModuleType.UMD：直接执行脚本（UMD 通常会挂到 `globalThis` 上）。
- JsModuleType.ESM：`CustomV8ModuleResolver` 编译并为 ESM 模块返回 IV8Module。
- JsModuleType.CJS：CommonJS 模块使用模拟的 `module.exports` 对象执行，导出作为模块对象返回。

### 构建和测试

- 从项目根目录运行：

```cmd
gradlew.bat clean assemble -x test
```

- 如果编译成功，通过启动主机应用程序或执行新服务的单元测试来在运行时测试功能。

### 注意事项和技巧

- 保持嵌入的 JS 文件相对较小，以保持 jar 大小可管理。
- 优先选择精简或压缩的 UMD 构建版本进行嵌入。
- 如果库暴露异步接口（Promise），Java 实现应使用 Javet 的 Promise 辅助工具或 `V8ValuePromise` 轮询等待结果。
- 添加单元测试，使用模拟或引擎池来验证解析/高亮行为。
