# HTML 页面压缩

`minify-html` 是独立的 Halo 插件，用于在服务端压缩前台 HTML 页面响应。

## 插件信息

| 项 | 值 |
| --- | --- |
| `metadata.name` | `minify-html` |
| `displayName` | `HTML 页面压缩` |
| Halo 要求 | `>=2.25.0` |
| 包路径 | `packages/halo-plugin-minify-html` |
| 版本来源 | `gradle.properties` 中的 `minifyHtmlVersion` |

## 行为范围

- 仅处理 `GET` 请求返回的 HTML 页面响应。
- 仅处理 `text/html` 且未经过 `gzip`、`br` 等编码的响应。
- 默认跳过后台、API、静态资源等路径，避免误处理非页面响应。
- 非 UTF-8 响应会自动跳过。
- 压缩失败时自动回退原始 HTML，不影响页面正常返回。

## 版本变体

| 制品 | 说明 |
| --- | --- |
| `minify-html-all-platforms-<version>.jar` | 包含全部支持平台的 native 依赖。 |
| `minify-html-<platform>-<version>.jar` | 只包含目标平台 native 依赖。 |

当前平台维度包括 Linux arm64、Linux x86_64、macOS arm64、macOS x86_64、Windows x86_64。

## 配置

在插件设置的“HTML 页面压缩”分组中启用“自动压缩”。如站点已由 CDN、反向代理或上游网关统一处理 HTML 压缩，应避免重复启用。
