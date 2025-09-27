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

构建完成后，可以在 `build/libs` 目录找到插件 jar 文件。

## 如何添加新的嵌入式 JS 模块

本项目将 JavaScript 工具嵌入到 Java 运行时中，并将其预加载到 Javet V8 运行时中。按照以下步骤添加对新 JS 模块的支持。

### 在 `JsModule` 枚举中添加条目

- 文件：`src/main/java/top/howiehz/halo/plugin/extra/api/service/js/module/JsModule.java`
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

- 文件：`src/main/java/top/howiehz/halo/plugin/extra/api/service/js/CustomJavetEngine.java`
- 引擎当前在 `preloadModules()` 中预加载 `Shiki`
- 使用 `JsModule.MARKED.getSourceCode()` 读取资源，使用 `v8Runtime.getExecutor(code).executeVoid()` 执行。
- 加载后，验证预期的函数是否暴露在 `globalThis`（或其他入口点）上。保持预加载对错误的容忍性，避免引擎创建失败。

### 暴露 Java 服务来调用模块

- 在 `service/js/<module>` 下创建服务接口（示例 `service/js/marked/MarkedService.java`），定义您需要的操作。
- 使用 Spring `@Service` 类实现接口，该类使用现有的 `V8EnginePoolService` 对运行时执行调用，类似于 `ShikiHighlightServiceImpl`。
- 优先读取 `globalThis` 函数（例如 `parseMarkdown`）或 `globalThis.<lib>.parse`。

### 验证

- 使用 `V8EnginePoolService.executeScript` 或 `withEngine` 调用函数并验证结果。
- 在 `CustomJavetEngine.preloadModules()` 中添加快速布尔检查，如 `typeof parseMarkdown === 'function'` 并记录结果。

### 模块类型和自定义解析器

- JsModuleType.UMD：直接执行脚本（UMD 通常附加到 globalThis）。
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
- 如果库暴露异步 API（promises），Java 实现应该使用 Javet Promise 助手或 V8ValuePromise 轮询来等待 promise 结果。
- 添加单元测试，使用模拟或引擎池来验证解析/高亮行为。