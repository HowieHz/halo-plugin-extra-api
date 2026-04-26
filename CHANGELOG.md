# Changelog

<!-- markdownlint-disable MD024 -->

<!--
三级标题顺序

### 💥 破坏性变更
### 🚀 新功能
### 🔧 代码重构
### ⚠️ 弃用
### 🗑️ 移除
### 🐛 错误修复
### 🔒 安全
### 📄 文档
### 🛠️ 杂项维护
-->

本项目的所有显著变更将记录在本文件中。

该格式基于[保持变更日志](https://keepachangelog.com/en/1.1.0)，
本项目遵循[语义版本管理](https://semver.org/spec/v2.0.0.html)。


## [Unreleased]

- 待补充本次版本说明。

## [3.1.1] - 2026-04-22

### 🛠️ 杂项维护

- 更新依赖
- Halo CMS 最低版本要求更改为 2.24.0

## [3.1.0] - 2026-04-14

### 🚀 新功能

- 新增 HTML 页面压缩处理器

### 🛠️ 杂项维护

- 更新依赖

## [3.0.4] - 2026-03-14

### 🛠️ 杂项维护

- 更新依赖
- Halo CMS 最低版本要求更改为 2.23.0

## [3.0.3] - 2026-02-28

### 🔧 优化改进

- 适配新版本 shiki

### 🛠️ 杂项维护

- 更新依赖

## [3.0.2] - 2026-02-11

### 🔧 优化改进

- 适配新版本 shiki

### 🛠️ 杂项维护

- 更新全部依赖，优化性能表现

## [3.0.1] - 2025-12-30

### 🔧 优化改进

- 适配 Halo CMS 2.22

### 🛠️ 杂项维护

- 更新全部依赖

## [3.0.0] - 2025-11-06

### 💥 破坏性变更

- Finder API 重命名: `extraApiRenderFinder.renderCodeHtml` -> `extraApiJsRenderFinder.highlightCodeInHtml`

### 🚀 新功能

- 新增中英文混排格式化 API
- 新增中英文混排格式化文章/单页处理器

## [2.2.1] - 2025-11-05

### 🔧 优化改进

- 更新依赖 shiki 到 3.14.0 版本

### 🛠️ 杂项维护

- 优化文档

## [2.2.0] - 2025-10-01

### 🚀 新功能

- 添加 HTML 内容字数统计 API（轻量版可用）
  - 提供 HTML 内容字数统计功能，支持统计任意 HTML 字符串的字数。适用于统计文章内容、瞬间内容或自定义 HTML 片段的字数。

### 🛠️ 杂项维护

- 优化文档

## [2.1.0] - 2025-10-01

### 🚀 新功能

- 允许配置 JS 引擎池中引擎数量，防止内存溢出。

### 🔧 优化改进

- 优化 Shiki 代码高亮的性能
  - 添加渲染缓存
  - 优化 DOM 插入性能
  - 降低通讯开销，将多个 JS 操作打包到一次执行

## [2.0.0] - 2025-09-29

### 💥 破坏性变更

- Finder API 重命名: `extraApiStatsFinder.postWordCount` -> `extraApiStatsFinder.getPostWordCount`

### 🚀 新功能

- 新增代码高亮 API
- 新增代码高亮文章/单页处理器

## [1.0.0] - 2025-09-21

### 🚀 新功能

- 新增文章字数计算 API

[Unreleased]: https://github.com/HowieHz/halo-plugin-extra-api/compare/v3.1.1...HEAD
[3.1.1]: https://github.com/HowieHz/halo-plugin-extra-api/compare/v3.1.0...v3.1.1
[3.1.0]: https://github.com/HowieHz/halo-plugin-extra-api/compare/v3.0.4...v3.1.0
[3.0.4]: https://github.com/HowieHz/halo-plugin-extra-api/compare/v3.0.3...v3.0.4
[3.0.3]: https://github.com/HowieHz/halo-plugin-extra-api/compare/v3.0.2...v3.0.3
[3.0.2]: https://github.com/HowieHz/halo-plugin-extra-api/compare/v3.0.1...v3.0.2
[3.0.1]: https://github.com/HowieHz/halo-plugin-extra-api/compare/v3.0.0...v3.0.1
[3.0.0]: https://github.com/HowieHz/halo-plugin-extra-api/compare/v2.2.1...v3.0.0
[2.2.1]: https://github.com/HowieHz/halo-plugin-extra-api/compare/v2.2.0...v2.2.1
[2.2.0]: https://github.com/HowieHz/halo-plugin-extra-api/compare/v2.1.0...v2.2.0
[2.1.0]: https://github.com/HowieHz/halo-plugin-extra-api/compare/v2.0.0...v2.1.0
[2.0.0]: https://github.com/HowieHz/halo-plugin-extra-api/compare/v1.0.0...v2.0.0
[1.0.0]: https://github.com/HowieHz/halo-plugin-extra-api/releases/tag/v1.0.0
