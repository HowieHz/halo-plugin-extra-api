# 发布流程

## 插件 Release

稳定版发布由仓库 Release workflow 构建并上传 JAR 制品。

发布前应确认：

- `gradle.properties` 中的 `version` 已更新为 Extra API 版本
- `gradle.properties` 中的 `nodejsRuntimeVersion` 已更新为 Node.js Runtime / API 版本
- `gradle.properties` 中的 `minifyHtmlVersion` 已更新为 Minify HTML 版本
- `packages/halo-plugin-extra-api/CHANGELOG.md` 已记录 Extra API 版本变更
- `packages/halo-plugin-nodejs-runtime/CHANGELOG.md` 已记录 Node.js Runtime 版本变更
- `packages/halo-plugin-minify-html/CHANGELOG.md` 已记录 Minify HTML 版本变更
- `.\gradlew test buildAll --console=plain` 通过
- Full 版 Extra API 的 manifest 会声明 `nodejs-runtime` 前置依赖
- Lite 版 Extra API 不声明 `nodejs-runtime` 前置依赖

GitHub Release 使用包级 tag：

- Extra API: `halo-plugin-extra-api@<version>`
- Node.js Runtime: `halo-plugin-nodejs-runtime@<nodejsRuntimeVersion>`
- Node Runtime API: `nodejs-runtime-api@<nodejsRuntimeVersion>`
- Minify HTML: `halo-plugin-minify-html@<minifyHtmlVersion>`

Release PR 的 guard 评论会比较 base 分支和当前分支的版本字段，并列出检测到的待发布包。Extra API 资产使用 `version`，例如 `extra-api-full-linux-x86_64-3.1.7.jar`；Node.js Runtime 资产使用 `nodejsRuntimeVersion`，例如 `nodejs-runtime-linux-x86_64-1.0.0.jar`；Minify HTML 资产使用 `minifyHtmlVersion`，例如 `minify-html-linux-x86_64-1.0.0.jar`。

## Node Runtime API

`nodejs-runtime-api` 发布到 GitHub Packages。Release 发布或手动触发 `Publish Node.js Runtime API` workflow 后，会执行：

```powershell
.\gradlew :nodejs-runtime-api:publish --console=plain
```

默认仓库：

```text
https://maven.pkg.github.com/HowieHz/halo-plugins
```

## 文档站

文档站使用 VitePress 构建，base path 为 `/halo-plugins/`。

`Build and Deploy Docs` workflow 会构建 `docs/.vitepress/dist`，部署时通过 Halo Static Pages CLI 上传到站点页面。
