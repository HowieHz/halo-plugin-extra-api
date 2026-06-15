# 主题接入

主题接入 Extra API 时，核心原则是先检测插件，再按版本能力调用 Finder。

## 检测插件

```html
<th:block th:if="${pluginFinder.available('extra-api')}">
  <!-- Extra API 可用时渲染 -->
</th:block>
```

如果主题只兼容某个大版本，可以传入版本约束。

```html
<th:block th:if="${pluginFinder.available('extra-api', '3.*')}">
  <!-- 仅在 3.x 可用时渲染 -->
</th:block>
```

## 区分轻量版和全量版

全量版功能需要 `extraApiPluginInfoFinder.isFullVersion()` 返回 `true`。

```html
<th:block th:if="${pluginFinder.available('extra-api') and extraApiPluginInfoFinder.isFullVersion()}">
  <div th:utext="${extraApiJsRenderFinder.highlightCodeInHtml(post.content?.content)}"></div>
</th:block>
```

轻量版可以使用核心统计和排版能力，但不能调用 JavaScript 互操作 Finder。

## 推荐降级策略

- 字数统计、阅读时间等增强信息可以直接隐藏。
- 代码高亮可以回退到主题原有的前端高亮方案。
- HTML 压缩是服务端过滤能力，主题通常不需要感知。

不要在插件不可用时直接调用 Finder。Halo 模板解析阶段会因为 Finder 不存在而中断页面渲染。
