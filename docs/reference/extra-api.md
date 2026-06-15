# Extra API

`extra-api` 是面向 Halo 主题和站点的能力扩展插件。

## 插件信息

| 项 | 值 |
| --- | --- |
| `metadata.name` | `extra-api` |
| `displayName` | `API 扩展包` |
| Halo 要求 | `>=2.25.0` |
| 包路径 | `packages/halo-plugin-extra-api` |

## 功能范围

无需主题适配即可启用的功能：

- 中英文混排格式化处理器
- 代码高亮处理器，仅全量版

提供给主题开发者的 Finder API：

- `extraApiPluginInfoFinder`
- `extraApiStatsFinder`
- `extraApiRenderFinder`
- `extraApiJsRenderFinder`，仅全量版

## 版本类型

`extraApiPluginInfoFinder` 提供以下检测方法：

```javascript
extraApiPluginInfoFinder.isFullVersion()
extraApiPluginInfoFinder.isLiteVersion()
extraApiPluginInfoFinder.getVersionType()
extraApiPluginInfoFinder.isJavaScriptAvailable()
```

## Full 与 Lite 的差异

| 能力 | Lite | Full |
| --- | --- | --- |
| 基础 Finder | 支持 | 支持 |
| 中英文混排 | 支持 | 支持 |
| 字数统计 | 支持 | 支持 |
| Shiki 代码高亮 | 不支持 | 支持 |
| `nodejs-runtime` 前置依赖 | 不需要 | 需要 |

详细 Finder 参数和示例仍保留在仓库 README 中，后续可以继续拆分到本站参考页。
