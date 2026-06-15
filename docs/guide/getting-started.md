# 快速开始

本仓库维护一组 Halo CMS 插件。普通站点安装插件时只需要下载 Release 里的 JAR；只有插件开发者才需要依赖 `nodejs-runtime-api`。

## 环境要求

- Halo CMS `>= 2.25.0`
- 插件运行环境使用 Java 21
- 本仓库构建使用 Gradle Wrapper

## 安装 Extra API

1. 前往 GitHub Releases 下载 `extra-api-lite-*.jar` 或 `extra-api-full-*.jar`。
2. 在 Halo 后台的插件页面上传 JAR。
3. 启用插件后，在主题模板里通过 Finder API 使用对应能力。

轻量版不包含 JavaScript 互操作能力，不需要安装 `nodejs-runtime`。全量版包含 Shiki 代码高亮能力，启用前需要先安装并启用满足前置依赖版本要求的 `nodejs-runtime` 插件。HTML 页面压缩已拆分为独立的 `minify-html` 插件。

## 安装 Node.js 运行时

1. 前往 GitHub Releases 下载 `nodejs-runtime-*.jar`。
2. 按服务器平台选择平台包，或使用 `nodejs-runtime-all-platforms-*.jar`。
3. 在 Halo 后台上传并启用，随后再启用依赖它的插件。

`nodejs-runtime` 是运行时插件，不直接面向主题模板暴露展示功能。

## 安装 HTML 页面压缩

1. 前往 GitHub Releases 下载 `minify-html-*.jar`。
2. 按服务器平台选择平台包，或使用 `minify-html-all-platforms-*.jar`。
3. 在 Halo 后台上传并启用，然后在插件设置中开启“自动压缩”。

## 主题侧检测

主题应先检测插件是否可用，再调用 Extra API 的 Finder。

```html
<th:block th:if="${pluginFinder.available('extra-api')}">
  <span th:text="|总字数：${extraApiStatsFinder.getPostWordCount()}|"></span>
</th:block>
```

如果需要使用全量版能力，建议同时检查版本类型。

```html
<th:block th:if="${pluginFinder.available('extra-api') and extraApiPluginInfoFinder.isFullVersion()}">
  <div th:utext="${extraApiJsRenderFinder.highlightCodeInHtml(post.content?.content)}"></div>
</th:block>
```
