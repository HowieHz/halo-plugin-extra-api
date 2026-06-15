# Node Runtime 前置插件设计

本文记录将 Node/Javet 运行时从 `extra-api` 拆为独立前置插件的设计约定。

## 目标

- `nodejs-runtime` 作为公共前置插件，提供 Node/Javet engine pool 和 JS 调用能力。
- 附属插件使用 ESM 编写源码，在构建时打包成单文件 CommonJS-compatible bundle。
- `nodejs-runtime` 只执行稳定 bundle，不支持运行时 `require`、`node_modules`、Node 内置模块解析。
- Javet 类型不暴露给附属插件。调用方不接触 `V8Runtime`、`JavetException`、Promise 细节。

## 插件分层

- `nodejs-runtime-api`: 发布给其他插件编译依赖，包含 API 接口和数据结构。
- `nodejs-runtime`: Halo 插件，实现 engine pool、模块注册、执行、维护接口。
- 附属插件: 实现 `NodeModuleProvider`，通过 `NodeRuntime.call(...)` 调用自己的或其他插件的模块。

## API 草案

```java
public interface NodeRuntime extends ExtensionPoint {
    Mono<String> call(NodeCall call);

    Mono<NodeRuntimeStats> stats();
}

public interface NodeRuntimeMaintenance extends ExtensionPoint {
    Mono<NodeRuntimeRebuildResult> rebuildRuntime();
}

public interface NodeModuleProvider extends ExtensionPoint {
    PluginContext pluginContext();

    List<NodeModuleDescriptor> modules();
}
```

```java
public record NodeModuleRef(
    String pluginName,
    String pluginVersion,
    String moduleId
) {
    public static NodeModuleRef of(PluginContext pluginContext, String moduleId) {
        return new NodeModuleRef(pluginContext.getName(), pluginContext.getVersion(), moduleId);
    }
}

public record NodeCall(
    NodeModuleRef module,
    String functionName,
    String argsJson,
    Duration timeout
) {
}

public record NodeModuleDescriptor(
    String moduleId,
    String source,
    String integrity
) {
}

public record NodeRuntimeStats(
    int poolMinSize,
    int poolMaxSize,
    int activeEngineCount,
    int idleEngineCount,
    int queuedCallCount,
    int registeredModuleCount
) {
}

public record NodeRuntimeRebuildResult(
    int registeredModuleCount,
    int closedEngineCount,
    Duration elapsed
) {
}
```

`NodeModuleDescriptor` 应提供 `fromResource(...)` 工厂方法，从 provider 所在插件的 classloader 读取 bundle 并计算 `sha256` integrity。

## 模块 ID

附属插件只声明本地 `moduleId`。`node-runtime` 通过 provider 的 `PluginContext` 或调用方提供的 `NodeModuleRef` 生成完整模块 ID:

```text
@<plugin-name>/<module-id>@<plugin-version>
```

示例:

```text
@extra-api/shiki@3.2.1
@other-plugin/markdown@1.0.0
```

本地 `moduleId` 规则:

- 允许: `A-Z`、`a-z`、`0-9`、`-`、`_`、`.`、`/`
- 不允许: 空格、`@`、连续 `/`、开头 `/`、结尾 `/`
- 大小写敏感

建议校验:

```regex
^(?!/)(?!.*//)(?!.*/$)[A-Za-z0-9._/-]+$
```

`pluginName` 使用 Halo `metadata.name` 规则。`pluginVersion` 使用 `PluginContext.getVersion()` 原值，但不允许空格、`/`、`@`。

## JS 模块格式

开发者可以用 ESM 写源码:

```js
import { codeToHtml } from "shiki/bundle/full";

export async function highlightCodeBatch(input) {
  return {};
}
```

构建产物必须是单文件 CommonJS-compatible bundle。运行时契约是 `module.exports`:

```js
module.exports = {
  async highlightCodeBatch(input) {
    return {};
  }
}
```

UMD bundle 只要能在 CommonJS 环境下导出 `module.exports`，也可以使用。当前 Shiki 的 `shiki.umd.cjs` 可先保留，迁移时由 runtime 通过 CommonJS wrapper 加载，不再依赖 `globalThis` 函数。

v1 不支持:

- ESM 作为运行时 ABI
- `require(...)`
- `node_modules` 解析
- Node 内置模块
- 动态 `import(...)`
- 多文件 chunk

所有第三方 JS 依赖必须在构建阶段 bundle 进单文件产物。

## 函数调用

`functionName` 必须等价于普通 JS 函数声明中的合法 `Identifier`:

```js
function name() {}
```

规则:

- 不能以数字开头
- 可以用字母、数字、`_`、`$`
- 可以用 Unicode 字符，包括中文
- 不能包含空格、`-`、`.`、`@` 等符号
- 不能使用保留字
- 严格模式下避开 `yield`、`await` 等特殊关键字
- 大小写敏感

调用时只传一个参数:

```js
const input = JSON.parse(argsJson);
const result = await exports[functionName](input);
return JSON.stringify(result);
```

导出函数必须接收单个 `input` 对象。返回值必须是 JSON 可序列化值。不可返回 `function`、`symbol`、`undefined`、`BigInt`、循环引用对象或原生对象。

JS `throw` 或 Promise reject 时，`Mono` 失败并抛出统一 `NodeRuntimeException`，包含 `moduleId`、`functionName`、JS error message 和可用的 JS stack trace。不要把错误包装成成功 JSON 返回。

## 模块生命周期术语

- discover: 扫描当前启用插件的 `NodeModuleProvider`。
- register: 把 `NodeModuleDescriptor` 记入 registry，只保存 `source` / `integrity`，不执行 JS。
- load: 把某个模块 source 真正执行进某个 engine，得到 `module.exports`。

对外只有 `call(...)`。内部建议分为两个步骤:

```text
syncRegistryIfNeeded()
ensureModuleLoaded(engine, moduleId)
```

场景规则:

- 第一次调用某模块: discover -> register -> load requested module -> invoke。
- 后续同 engine 调用同模块: 直接 invoke。
- registry 有模块但当前 engine 没加载: load requested module -> invoke。
- 后续安装/启用新插件: 调用新模块时 discover/register 新模块，追加 registry，不重建 pool。
- 插件更新且 module ID 带新版本: 注册新完整 ID，旧版本保留到 `rebuildRuntime()`。
- 同完整 module ID + 同 integrity: 幂等。
- 同完整 module ID + 不同 integrity: 拒绝注册并报错。

## rebuildRuntime

`rebuildRuntime()` 是站长/维护接口，不是附属插件作者的常规调用接口。

语义:

- 等正在执行的调用结束。
- 排队中的调用等待重建完成后继续执行。
- 重新 discover 当前 provider。
- 用当前 provider 结果替换整个 registry。
- 关闭旧 engine pool。
- 创建新 engine pool。
- 不立即 load 所有 JS。后续调用按需 load。

并发模型:

- call 执行阶段持有 read lock。
- rebuildRuntime 持有 write lock。
- write lock 等 active calls 完成。
- queued calls 尚未进入执行阶段，不持有 read lock。
- rebuild 完成后 queued calls 使用新 pool 执行。

建议站长在以下场景执行 `rebuildRuntime()`:

- 插件卸载/禁用后清理旧模块。
- 模块状态异常。
- 想清理旧版本模块。
- 运行时配置调整后需要重建 pool。

## timeout 和队列

配置项归 `node-runtime` 插件自己的 Setting / ConfigMap 管理。

v1 配置:

- `poolMinSize = 1`
- `poolMaxSize = 2`
- `defaultTimeout = 30s`
- `maxTimeout = -1`

help 文案必须说明:

- `poolMinSize`: 最小引擎数量。数值越大，空闲时内存占用越多。
- `poolMaxSize`: 最大引擎数量。数值越大，并发 JS 调用能力越强，但 CPU 和内存占用越高。
- `defaultTimeout`: 调用方没有指定 timeout 时使用的默认执行超时时间。
- `maxTimeout`: 调用方允许指定的最大 timeout。`-1` 表示不限制最大值。它不是默认超时时间，也不表示禁用超时。

v1 不支持单次调用永不超时。每次 call 最终必须有一个正数 timeout:

```text
call.timeout != null -> 使用 call.timeout
call.timeout == null -> 使用 defaultTimeout
```

timeout 语义:

- 队列等待不计入 timeout。
- 首次 module load 计入 timeout。
- JS 函数执行计入 timeout。
- Promise await 计入 timeout。
- `JSON.stringify` 计入 timeout。

v1 使用 unbounded queue。`poolMaxSize` 只限制同时运行的调用数量，不限制等待调用数量。文档必须说明: 如果调用量长期超过处理能力，等待队列可能增长并占用内存。第一版为了配置简单暂不提供 `maxQueueSize`。

## 日志

v1 支持基础 `console.*` 到 Halo logger 的映射:

- `console.debug` -> debug
- `console.log` / `console.info` -> info
- `console.warn` -> warn
- `console.error` -> error

日志应带完整 module ID，例如:

```text
[node-runtime] [@extra-api/shiki@3.2.1] render started
```

不在 v1 做实时日志订阅、结构化事件流或前端日志控制台。
