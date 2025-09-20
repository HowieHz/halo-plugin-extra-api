# halo-plugin-extra-api

halo-plugin-extra-api - Halo 插件

## 简介

一个为 Halo CMS 提供额外 API 的轻量级插件。

## 已提供 API

### 文章字数统计

本插件提供了一个 API 用于查询文章字数，可查询指定文章/全部文章总和。

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

## 许可证

[AGPL-3.0](./LICENSE) © HowieHz 