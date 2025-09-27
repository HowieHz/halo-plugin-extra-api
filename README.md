# halo-plugin-extra-api

## 简介

一个为 Halo CMS 提供额外 API 的轻量级插件。

## 核心理念

> CMS 的价值就是在服务端管理和处理内容，为什么要把简单的数据处理推到前端去增加复杂度呢？

让 CMS 回归"内容即数据"的本质，减少**不必要**的前端异步请求和动态渲染。

这个插件正是基于这个理念：

- 让复杂的逻辑在后端处理
- 前端模板只负责展示
- 为主题提供简洁的 Finder API
- 减少不必要的 JavaScript 依赖

<details><summary>前端动态加载方式 vs 后端服务端渲染方式</summary>

| 对比维度                   | 前端动态加载方式                 | 后端服务端渲染方式              |
|------------------------|--------------------------|------------------------|
| **全页无刷兼容性（Pjax/Swup）** | ❌ 需要额外处理动态内容加载           | ✅ 模板渲染，天然支持无刷          |
| **性能表现**               | ❌ 需要额外 HTTP 请求，增加延迟      | ✅ 服务端渲染，一次性输出          |
| **用户体验**               | ❌ 页面闪烁，先显示占位符后填充数据       | ✅ 内容立即可见，无加载状态         |
| **SEO 友好**             | ❌ 搜索引擎难以抓取动态内容           | ✅ 服务端渲染，完全 SEO 友好      |
| **错误处理**               | ❌ 需要处理网络失败、超时等异常         | ✅ 服务端统一异常处理，减轻主题作者心智负担 |
| **开发复杂度**              | ❌ 需要编写 JS 代码、状态管理、DOM 操作 | ✅ 模板中直接调用，代码简洁         |
| **缓存策略**               | ❌ 需要前端缓存逻辑或重复请求          | ✅ 可利用模板缓存和服务端缓存        |
| **首屏渲染 (FCP)**         | ❌ 需要等待 JS 执行和 API 响应     | ✅ HTML 直接包含内容，渲染更快     |
| **最大内容绘制 (LCP)**       | ❌ 动态内容加载延迟主要内容显示         | ✅ 关键内容随页面一起渲染          |
| **累积布局偏移 (CLS)**       | ❌ 内容异步加载可能导致页面跳动         | ✅ 静态布局，无意外的布局变化        |
| **交互响应 (INP)**         | ❌ JS 执行和 DOM 操作影响交互性能    | ✅ 减少 JS 负担，交互更流畅       |

</details>

## TODO

<details><summary>展开折叠内容</summary>

- [ ] 提供随机文章 API
- [ ] 提供预计阅读时间 API，及相关配置项
- [ ] 提供图表渲染 API
- [ ] 提供公式渲染 API
- [ ] 分离 Node.js 环境支持为可选前置插件（预计 2.0 版本实现）

</details>

## 快速跳转

- [halo-plugin-extra-api](#halo-plugin-extra-api)
  - [简介](#简介)
  - [核心理念](#核心理念)
  - [TODO](#todo)
  - [快速跳转](#快速跳转)
  - [处理器文档](#处理器文档)
    - [代码高亮（通过 Shiki.js 渲染）](#代码高亮通过-shikijs-渲染)
      - [特点](#特点)
      - [配置选项](#配置选项)
      - [支持的主题](#支持的主题)
      - [补充说明](#补充说明)
  - [Finder API 文档](#finder-api-文档)
    - [检测本插件是否启用](#检测本插件是否启用)
    - [统计信息 API](#统计信息-api)
      - [文章字数统计](#文章字数统计)
    - [渲染 API](#渲染-api)
      - [代码高亮（通过 Shiki.js 渲染）](#代码高亮通过-shikijs-渲染-1)
  - [开发指南/贡献指南](#开发指南贡献指南)
  - [许可证](#许可证)

## 处理器文档

### 代码高亮（通过 Shiki.js 渲染）

插件提供了自动化的代码高亮处理器，无需在模板中手动调用，即可对文章和页面内容中的代码块进行语法高亮渲染。

#### 特点

- 双主题支持：
    - 可同时渲染浅色和深色主题，便于主题切换
- 语言自动检测：
    - 从 `class` 属性中提取语言标识（如 `language-java`、`lang-python`）
- 容错处理：
    - 渲染失败时保持原始代码块不变
- 性能说明：
    - 使用 V8 引擎池和异步处理，提升渲染效率
- 默认自动渲染范围：
  - 处理器会自动处理以下页面内容并在页面 `head` 注入自定义 CSS 样式：
      - 文章内容 (`post`)
      - 页面内容 (`page`)

#### 配置选项

- 自动渲染: 启用之后会自动渲染文章和单页中的代码块。注：Finder API 渲染不受此配置项影响。
- 自定义注入 CSS 样式：启用自动渲染时将在页面 head 注入样式以优化 Shiki 渲染效果，默认值提供了边距调整/行号显示/基于媒体查询的明暗切换功能。
    - 额外注入规则: 指定额外的页面路径规则，支持通配符。
        - 默认包含: `/moments/**`, `/docs/**`
        - 支持自定义路径，如 `/custom/**`
- 明暗双倍渲染模式切换：
    - 单主题模式:
        - 主题: 选择单个代码高亮主题
    - 双主题模式:
        - 亮色主题: 浅色模式使用的主题
        - 暗色主题: 深色模式使用的主题
        - 亮色主题代码块类名: 浅色代码块的 CSS 类名
        - 暗色主题代码块类名: 深色代码块的 CSS 类名

#### 支持的主题

插件内置了 118 个 Shiki 主题，包括：

- GitHub Light/Dark 系列
- One Light/Dark Pro
- Nord, Dracula, Monokai
- Material Theme 系列
- Catppuccin 系列

完整主题列表: 请在 Halo 管理后台的插件设置页面查看所有可用主题。或前往[官方文档](https://shiki.zhcndoc.com/themes)在线预览。

#### 补充说明

- 工作原理：
  1. 内容处理阶段：在内容渲染时，处理器会扫描 HTML 中的 `<pre><code>` 结构
  2. 语言检测：从 `class` 属性中提取语言标识（如 `language-java`、`lang-python`）
    ```html
    <!-- 标准 Markdown 格式 -->
    <pre><code class="language-java">public class Hello { }</code></pre>
    
    <!-- 简写格式 -->
    <pre><code class="lang-python">print("Hello World")</code></pre>
    
    <!-- 直接在 pre 标签上指定 -->
    <pre class="language-javascript"><code>console.log("Hello");</code></pre>
    ```
  3. 主题应用：根据配置应用相应的 Shiki 主题进行高亮
  4. 样式注入：在页面头部注入配置的 CSS
- 错误处理：
  - 不支持的语言/主题会被跳过渲染
  - 渲染失败时保留原始格式
- 性能说明：
  - 使用 V8 引擎池和异步处理，提升渲染效率
- 补充说明：
  - 双主题模式会生成两个并列的 div 元素 

## Finder API 文档

### 检测本插件是否启用

**描述**

检测 ExtraAPI 插件是否已安装并启用。建议在主题中使用本插件 API 前先进行检测，以确保插件可用性。

**参数**

- `extra-api` - 本插件的标识符（`metadata.name`）

**返回值**

- `boolean` - 插件可用时返回 true，否则返回 false

**示例**

```html
<!--/* 先检测插件可用性，再使用 API */-->
<th:block th:if="${pluginFinder.available('extra-api')}">
    <span
            th:text="|总字数：${extraApiStatsFinder.getgetPostWordCount()}|"
    ></span>
</th:block>

<!--/* 写在一个标签内也可以，th:if 的优先级比 th:text 高 */-->
<span
        th:if="${pluginFinder.available('extra-api')}"
        th:text="|总字数：${extraApiStatsFinder.getPostWordCount()}|"
></span>

<!--/* 自然模板写法 */-->
<span th:if="${pluginFinder.available('extra-api')}">总字数：[[${extraApiStatsFinder.getPostWordCount()}]]</span>
```

**说明**

使用 `pluginFinder.available('extra-api')` 可以优雅地处理插件依赖，避免在插件未安装时出现模板错误，提升主题的兼容性和用户体验。

### 统计信息 API

**Finder 名称：** `extraApiStatsFinder`

#### 文章字数统计

```javascript
extraApiStatsFinder.getPostWordCount({
  name: 'post-metadata-name',  // 可选，未传入则统计全部文章字数总和
  version: 'release' | 'draft'  // 可选，默认 'release'
});
```

```javascript
extraApiStatsFinder.getPostWordCount();
```

**参数**

- `name:string` – 文章 `metadata.name`（可选，不传则统计全站）
- `version:string` – 统计版本，可选 `release`（默认）或 `draft`

**返回值**

- `int` – 字数统计结果（非负），不存在或参数缺失时返回 0

**描述**

- 参数说明：
    - `name`：文章的 `metadata.name`，可选参数。未传入时统计全部文章字数总和。
    - `version`：统计版本，可选 `release`（默认）或 `draft`。
- 计数规则：
    - 中文、日文、韩文等 CJK 字符按每个字符计 1。
    - ASCII 连续字母/数字按 1 个单词计数。
    - 标点符号和空格不计入统计。
- 错误处理：
    - 输入为空或文章不存在时返回 0，不会抛出异常。
- 性能说明：
    - 单次调用开销较小，适合在模板中直接使用。
    - 启动时自动计算并缓存，仅在文章内容更新时重新计算。

**使用示例**

```html
<!--/* 统计文章已发布版本的字，下面这段代码可直接用于 /templates/post.html */-->
<span th:text="${extraApiStatsFinder.getPostWordCount({name: post.metadata.name})}"></span>

<!--/* 统计文章最新版本的字数（含草稿），下面这段代码可直接用于 /templates/post.html */-->
<span th:text="${extraApiStatsFinder.getPostWordCount({name: post.metadata.name, version: 'draft'})}"></span>

<!--/* 统计全站已发布文章的总字数，下面这段代码可直接用于全部模板 */-->
<span th:text="${extraApiStatsFinder.getPostWordCount()}"></span>
<!--/* 与下方写法等价 */-->
<span th:text="${extraApiStatsFinder.getPostWordCount({})}"></span>

<!--/* 统计全站所有文章最新版本的总字数（含草稿），下面这段代码可直接用于全部模板 */-->
<span th:text="${extraApiStatsFinder.getPostWordCount({version: 'draft'})}"></span>
```

### 渲染 API

**Finder 名称：** `extraApiRenderFinder`

#### 代码高亮（通过 Shiki.js 渲染）

```javascript
extraApiRenderFinder.renderCodeHtml(htmlContent)
```

**参数**

- `htmlContent:string` – HTML 内容

**返回值**

- `string` – 渲染后的 HTML 内容，渲染样式已内联，渲染失败时返回原始内容

**描述**

- 参数说明：
  - `htmlContent`：包含代码块的 HTML 内容，通常是文章或页面的内容字段。
- 功能特性：
  - 同 [处理器文档 - 代码高亮（通过 Shiki.js 渲染）](#代码高亮通过-shikijs-渲染) 中描述的功能特性一致。也受同样的配置项影响。

**使用示例**

```html
<!--/* 渲染文章内容中的代码块，下面这段代码可直接用于 /templates/post.html */-->
<div th:utext="${extraApiRenderFinder.renderCodeHtml(post.content?.content)}"></div>

<!--/* 在模板中使用，下面这段代码可直接用于 /templates/moment.html */-->
<div th:with="renderedContent=${extraApiRenderFinder.renderCodeHtml(moment.spec.content?.html)}">
    <div th:utext="${renderedContent}"></div>
</div>
```

## 开发指南/贡献指南

参见 [CONTRIBUTING.md](./CONTRIBUTING.md)

## 许可证

[AGPL-3.0](./LICENSE) © HowieHz
