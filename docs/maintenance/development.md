# 开发

## 仓库结构

```text
packages/
  halo-plugin-extra-api/
  halo-plugin-nodejs-runtime/
  halo-plugin-minify-html/
  nodejs-runtime-api/
docs/
```

Gradle 项目入口位于仓库根目录，VitePress 文档入口位于 `docs/`。

## 构建插件

```powershell
$env:JAVA_HOME = "D:\ProgramFiles\Scoop\apps\zulufx21-jdk\current"
$env:CI = "true"
.\gradlew test buildAll --console=plain
```

只构建轻量版 Extra API：

```powershell
.\gradlew buildLite --console=plain
```

## 构建文档

```powershell
pnpm install
pnpm docs:build
```

本地预览：

```powershell
pnpm docs:preview
```

开发模式：

```powershell
pnpm docs:watch
```

文档站点的部署路径固定为：

```text
https://howiehz.top/halo-plugins/
```
