# 包与制品

仓库是 Gradle monorepo，核心包位于 `packages/`。

| 包 | 类型 | 用途 |
| --- | --- | --- |
| `packages/halo-plugin-extra-api` | Halo 插件 | 面向站点和主题的 Extra API 插件，版本来自 `version`。 |
| `packages/halo-plugin-nodejs-runtime` | Halo 插件 | 共享 Node/Javet 运行时前置插件，版本来自 `nodejsRuntimeVersion`。 |
| `packages/halo-plugin-minify-html` | Halo 插件 | HTML 页面压缩插件，版本来自 `minifyHtmlVersion`。 |
| `packages/nodejs-runtime-api` | Maven library | 给其他插件编译期依赖的 Java API。 |

## Extra API JAR

`extra-api` 提供两类构建变体。

| 制品 | 说明 |
| --- | --- |
| `extra-api-lite-<version>.jar` | 轻量版，不包含 interop 功能，不依赖 `nodejs-runtime`。 |
| `extra-api-full-all-platforms-<version>.jar` | 全平台完整版，包含 full 功能，包体更大。 |
| `extra-api-full-<platform>-<version>.jar` | 平台特定完整版，保留历史文件名用于发布兼容。 |

完整版的 `plugin.yaml` 会声明：

```yaml
pluginDependencies:
  nodejs-runtime: >=<nodejsRuntimeVersion>
```

轻量版不会声明该前置依赖。

## Node.js Runtime JAR

`nodejs-runtime` 同样提供全平台和平台特定包。

| 制品 | 说明 |
| --- | --- |
| `nodejs-runtime-all-platforms-<version>.jar` | 包含全部支持平台的 Javet native 依赖。 |
| `nodejs-runtime-<platform>-<version>.jar` | 只包含目标平台 Javet native 依赖。 |

当前平台维度包括 Linux arm64、Linux x86_64、macOS arm64、macOS x86_64、Windows x86_64。

`nodejs-runtime` 从 `1.0.0` 开始独立版本号，不再跟随 Extra API 的 `3.x` 版本。

## Minify HTML JAR

`minify-html` 提供全平台和平台特定包。

| 制品 | 说明 |
| --- | --- |
| `minify-html-all-platforms-<version>.jar` | 包含全部支持平台的 minify-html native 依赖。 |
| `minify-html-<platform>-<version>.jar` | 只包含目标平台 minify-html native 依赖。 |

`minify-html` 从 `1.0.0` 开始独立版本号。

## Node Runtime API Maven 包

`nodejs-runtime-api` 的 Maven 坐标：

```text
top.howiehz.halo.plugin.node.runtime:nodejs-runtime-api:<version>
```

`nodejs-runtime-api` 与 `nodejs-runtime` 使用同一个 `nodejsRuntimeVersion`。

默认发布到 GitHub Packages：

```text
https://maven.pkg.github.com/HowieHz/halo-plugins
```
