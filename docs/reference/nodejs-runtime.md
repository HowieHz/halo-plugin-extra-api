# Node.js 运行时

`nodejs-runtime` 是共享 Node/Javet 执行能力的 Halo 前置插件。

## 插件信息

| 项 | 值 |
| --- | --- |
| `metadata.name` | `nodejs-runtime` |
| `displayName` | `Node.js 运行时` |
| Halo 要求 | `>=2.25.0` |
| 包路径 | `packages/halo-plugin-nodejs-runtime` |
| 版本来源 | `gradle.properties` 中的 `nodejsRuntimeVersion` |

## 职责边界

`nodejs-runtime` 负责：

- 管理 Javet Node engine pool
- 收集实现 `NodeModuleProvider` 的插件模块
- 校验模块描述符和 integrity
- 执行 `NodeRuntime.call(...)`
- 提供运行时统计和重建能力

`nodejs-runtime` 不负责：

- 暴露主题 Finder API
- 动态解析 `node_modules`
- 让调用插件直接接触 Javet 类型
- 承担具体业务插件的模板渲染逻辑

## 模块调用模型

附属插件通过 `nodejs-runtime-api` 编译依赖实现模块注册，然后在运行时通过 Halo extension point 获取 `NodeRuntime`。

```java
Mono<String> result = nodeRuntime.call(new NodeCall(
    NodeModuleRef.of(pluginContext, "shiki"),
    "highlight",
    argsJson,
    Duration.ofSeconds(5)
));
```

模块源码由附属插件打包后提供。运行时只执行稳定 bundle，不在生产环境里安装依赖或解析源工程。
