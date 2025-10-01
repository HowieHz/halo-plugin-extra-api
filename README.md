# halo-plugin-extra-api

## 简介

一个为 Halo CMS 提供额外 API 的轻量级插件。

快捷跳转：[版本说明](#版本说明)/[全量版使用须知](#全量版使用须知)/[文档目录](#文档目录)

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

## 功能介绍

本插件现版本已提供以下功能：

- 无需主题适配即可使用的功能：
    - [代码高亮处理器](#代码高亮处理器)（仅全量版可用）
- 需要主题适配的 Finder API：
    - [文章字数统计 API](#文章字数统计-api)
    - [代码高亮 API](#代码高亮-api)（仅全量版可用）

未来将实现的功能：[TODO](#todo)

欢迎为此插件提 [Issue](https://github.com/HowieHz/halo-plugin-extra-api/issues/new)，任何你需要的功能都可以在此处提出，我将在能力范围内尽力实现。

💖 支持本项目: 如果你觉得这个插件有用，点个 [Star](https://github.com/HowieHz/halo-plugin-extra-api) 就是对我最大的鼓励!

感谢所有支持本项目的用户和开发者，也特别感谢 Halo CMS 团队为插件生态提供的优秀平台。

## 版本说明

插件提供两个版本：

- **全量版**：包含所有功能，包括代码高亮等依赖 JS 的相关功能。
- **轻量版**：轻量级版本，不包含 JS 相关功能和相关依赖。

### 轻量版的优势

- 更小的插件体积
- 更快的启动速度
- 更低的内存占用
- 更低的系统性能要求
- 支持全平台（全量版仅支持以下平台：Linux ARM64、Linux x86_64、macOS ARM64、macOS x86_64、Windows x86_64）

### 轻量版本缺少的功能

- 代码高亮（Shiki.js 渲染）

<!-- - 图表渲染（Mermaid）
- 公式渲染（KaTeX） -->

- 其他 JS 运行时相关功能

如果您需要上述功能，请使用全量版。

## 全量版使用须知

⏱️ **首次使用提示**: 全量版的 JavaScript 功能（如 Shiki 代码高亮）在首次调用时需要 1~3 秒进行环境初始化。这是正常现象，之后的调用速度会非常快。

> ⚠️ **重要**: 全量版依赖 Javet 加载 Node.js 原生库（基于 JNI），受 Halo 插件架构限制，存在[已知问题](#全量版已知问题)。

### 基本使用要求

1. **请勿热重载更新**: 请勿直接覆盖更新（如使用应用商店/插件列表的快捷更新操作）
2. **正确的更新流程**:
    - 方法一：停止插件 → 卸载插件 → **重启 Halo** → 安装新版本 → 启动插件
    - 方法二：停止插件 → 卸载插件 → 安装新版本 → **重启 Halo** → 启动插件
3. **卸载推荐做法**: 在卸载全量版后**重启 Halo** 确保原生库资源完全释放。

### 全量版已知问题

- 问题一：卸载后重新安装全量版插件，不重启就启用。调用 JS 相关 API 时会出现错误：
    - **首次安装并启动**: 正常工作
    - **禁用后重新启用**: 正常工作
    - **卸载后重新安装**: ❌ 会出现 `JavetException: Javet library is not loaded` 错误
    - **重启 Halo 后**: 恢复正常工作

#### 问题一解决方案

安装好新版本插件后**先别启用**！   
在启用新版本插件之前，请**先重启 Halo CMS**。

未来版本会将引擎作为前置依赖插件，更新本插件不再需要重启。

<details>
<summary>📖 技术原因分析（展开查看详细说明）</summary>

根据错误日志和 Halo 架构分析:

**问题根源**:

1. **Halo 插件架构**: 根据 [Halo 开发文档 - 插件架构](https://docs.halo.run/developer-guide/core/framework), Halo 使用
   Spring Plugin Framework 实现插件隔离
    - 每个插件拥有独立的 Spring ApplicationContext
    - 每个插件使用独立的 classloader 加载资源
    - 插件间通过 ExtensionPoint 机制通信
    - classloader 完全隔离,无法跨插件共享类或资源

2. **Javet 原生库加载机制**:
    - Javet 需要从 JAR 中提取原生库文件(`.dll`/`.so`/`.dylib`)到临时目录
    - JVM 通过 JNI 加载这些原生库文件
    - 原生库文件一旦加载,会被 JVM 锁定

3. **冲突发生过程**:
    - 安装插件时,Javet 提取原生库文件到 `C:\Users\用户名\AppData\Local\Temp\javet\进程ID\`（以 Windows 举例）
    - JVM 加载这些文件并锁定
    - 卸载插件时,虽然 classloader 被销毁,但原生库文件仍被 JVM 锁定,无法删除
    - 重新安装时,Javet 尝试提取新文件到同一位置
    - 因为文件被锁定,提取失败
    - Javet 初始化失败,报错 `Javet library is not loaded because <null>`

**典型错误日志解读**:

```
WARN - Failed to write to ...libjavet-node-windows-x86_64.v.4.1.7.dll because it is locked
ERROR - Native Library already loaded in another classloader
ERROR - JavetException: Javet library is not loaded because <null>
```

这三条日志清晰地展示了整个失败过程:

1. 尝试写入文件失败(文件被锁定)
2. 检测到库已在其他 classloader 中加载
3. 初始化失败，因为无法提取必要的库文件

**为什么重启有效**:

- 重启 Halo 会终止 JVM 进程
- JVM 终止时会释放所有文件锁
- 临时目录中的原生库文件会被清理
- 新的 Halo 进程启动后，Javet 可以正常提取和加载原生库

**为什么 Javet 文档中提到的 JVM 参数无效**:

`-Djavet.lib.loading.suppress.error=true` 这个参数的作用是:

- 抑制 Javet 在检测到"already loaded in another classloader"时的错误日志
- **但无法解决文件锁定问题**
- 当库文件无法提取时，Javet 根本无法完成初始化
- 因此该参数在这个场景下无效

**架构层面的限制**:

这个问题是 JVM/JNI + 插件 classloader 隔离的固有矛盾:

- Halo 的插件隔离设计保证了安全性和稳定性
- 但也导致原生库这类 JVM 级别资源难以管理
- 类似问题在所有使用 classloader 隔离的插件系统中都存在
- 这不是 Javet 或本插件的 bug，而是架构层面的限制

**相关技术文档**:

- [Javet - Load and Unload](https://www.caoccao.com/Javet/reference/resource_management/load_and_unload.html)
- [Javet Issue #124 - Classloader Reload](https://github.com/caoccao/Javet/issues/124)
- [Halo - Plugin Framework](https://docs.halo.run/developer-guide/core/framework)

</details>

## TODO

<details><summary>展开折叠内容</summary>

- [ ] 优化代码高亮 API，减少调用次数，降低调用开销
- [ ] 提供随机文章 API
- [ ] 提供预计阅读时间 API，及相关配置项
- [ ] 提供图表渲染 API
- [ ] 提供公式渲染 API
- [ ] 分离 Node.js 环境支持为可选前置插件（预计 3.0 版本实现）

</details>

## 文档目录

- [halo-plugin-extra-api](#halo-plugin-extra-api)
    - [简介](#简介)
    - [核心理念](#核心理念)
    - [功能介绍](#功能介绍)
    - [版本说明](#版本说明)
        - [轻量版的优势](#轻量版的优势)
        - [轻量版本缺少的功能](#轻量版本缺少的功能)
    - [全量版使用须知](#全量版使用须知)
        - [基本使用要求](#基本使用要求)
        - [全量版已知问题](#全量版已知问题)
            - [问题一解决方案](#问题一解决方案)
    - [TODO](#todo)
    - [文档目录](#文档目录)
    - [处理器文档](#处理器文档)
        - [代码高亮处理器](#代码高亮处理器)
            - [特点](#特点)
            - [配置选项](#配置选项)
            - [支持的主题](#支持的主题)
            - [补充说明](#补充说明)
    - [Finder API 文档](#finder-api-文档)
        - [插件本体信息相关 API](#插件本体信息相关-api)
            - [检测本插件是否启用](#检测本插件是否启用)
            - [插件版本检测 API](#插件版本检测-api)
        - [统计信息 API](#统计信息-api)
            - [文章字数统计 API](#文章字数统计-api)
        - [渲染 API](#渲染-api)
            - [代码高亮 API](#代码高亮-api)
    - [下载和安装](#下载和安装)
        - [稳定版](#稳定版)
        - [开发版](#开发版)
            - [下载步骤](#下载步骤)
    - [开发指南/贡献指南](#开发指南贡献指南)
    - [许可证](#许可证)

## 处理器文档

### 代码高亮处理器

插件提供了自动化的代码高亮处理器，无需在模板中手动调用，即可对文章和页面内容中的代码块进行语法高亮渲染。

此功能通过 Shiki.js 渲染，仅在[全量版](#版本说明)中可用。

#### 特点

- 双主题支持：
    - 可同时渲染浅色和深色主题，便于主题切换
- 语言自动检测：
    - 从 `class` 属性中提取语言标识（如 `language-java`、`lang-python`）
- 容错处理：
    - 渲染失败时保持原始代码块不变
- 默认自动渲染范围：
    - 处理器会自动处理以下页面内容并在页面 `head` 注入自定义 CSS 样式：
        - 文章内容 (`post`)
        - 页面内容 (`page`)
- 性能说明：
    - 命中缓存时响应速度极快（微秒级），首次渲染需要 1-3 秒初始化 JS 环境
    - 批量处理策略：
        - 智能分组：根据 V8 引擎池大小动态分配任务（例如 14 个任务 + 5 个引擎 → 5 组，每组 2-3 个任务）
        - 并行执行：多个任务组并行处理，充分利用多核性能
        - 批量渲染：同一组内的任务在单个引擎中通过一次 JS 通信批量处理，减少引擎切换开销
        - 优先缓存：先检查缓存，只对未命中的请求进行实际渲染
    - 缓存策略：
        - LRU + TTL 双重策略：最多缓存 10,000 个代码块，每个条目 24 小时自动过期
        - 智能去重：相同代码内容+语言+主题的重复渲染会自动去重，避免重复计算
        - 缓存键：基于代码内容的 SHA-256 哈希值，避免长代码占用过多内存

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

### 插件本体信息相关 API

#### 检测本插件是否启用

**描述**

检测 ExtraAPI 插件是否已安装并启用。建议在主题中使用本插件 API 前先进行检测，以确保插件可用性。
官方文档：[插件 Finder API](https://docs.halo.run/developer-guide/theme/finder-apis/plugin#availablepluginname-requiresversion)

**参数**

- `extra-api` - 本插件的标识符（`metadata.name`）

**返回值**

- `boolean` - 插件可用时返回 true，否则返回 false

**说明**

使用 `pluginFinder.available('extra-api')` 可以优雅地处理插件依赖，避免在插件未安装时出现模板错误，提升主题的兼容性和用户体验。
注：在此基础上可以使用 `pluginFinder.available('extra-api', '2.*')` 锁定大版本号，避免 API 破坏性更新时导致主题渲染报错。

**示例**

```html
<!--/* 先检测插件可用性，再使用 API */-->
<th:block th:if="${pluginFinder.available('extra-api')}">
    <span
            th:text="|总字数：${extraApiStatsFinder.getPostWordCount()}|"
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

#### 插件版本检测 API

**Finder 名称：** `extraApiPluginInfoFinder`

**描述**

提供插件版本类型检测功能，让主题或其他代码能够检测当前运行的是轻量版还是全量版插件，以便有条件地使用高级功能。
（注：以下四个 API 本质上是**同一个 API**，您可以选择使用其中**任何一个**进行主题编写。）

**API 方法**

```javascript
// 检查是否为全量版
extraApiPluginInfoFinder.isFullVersion()

// 检查是否为轻量版  
extraApiPluginInfoFinder.isLiteVersion()

// 获取版本类型字符串
extraApiPluginInfoFinder.getVersionType()

// 检查 JavaScript 功能是否可用
extraApiPluginInfoFinder.isJavaScriptAvailable()
```

**参数**

- 无

**返回值**

- `isFullVersion()` → `boolean` - 全量版时返回 true，轻量版时返回 false
- `isLiteVersion()` → `boolean` - 轻量版时返回 true，全量版时返回 false
- `getVersionType()` → `string` - 返回 "full" 或 "lite"
- `isJavaScriptAvailable()` → `boolean` - JavaScript 功能可用时返回 true

**描述**

- 检测原理
    - 通过检查 `V8EnginePoolService` 类是否存在来判断版本类型：
        - 全量版：包含 JavaScript 运行时，V8EnginePoolService 类存在
        - 轻量版：构建时排除 js 包下所有类，V8EnginePoolService 类不存在
- 应用场景
    - 主题兼容性：主题可以根据插件版本提供不同的功能体验
    - 用户提示：向用户说明当前版本的功能限制
    - 条件渲染：仅在支持的版本中启用特定功能（主题请求不存在的 Finder API 会报错并阻止页面渲染）

**使用示例**

```html
<!--/* 根据插件版本条件性显示功能 */-->
<th:block th:if="${extraApiPluginInfoFinder.isFullVersion()}">
    <div th:utext="${extraApiRenderFinder.renderCodeHtml(post.content?.content)}"></div>
</th:block>

<th:block th:unless="${extraApiPluginInfoFinder.isFullVersion()}">
    <p>当前使用轻量版插件，代码高亮功能不可用</p>
</th:block>

<!--/* 显示当前版本类型 */-->
<span>插件版本：[[${extraApiPluginInfoFinder.getVersionType()}]]</span>

<!--/* 检查 JavaScript 功能可用性 */-->
<div th:if="${extraApiPluginInfoFinder.isJavaScriptAvailable()}">
    <p>JavaScript 功能可用，支持高级渲染功能</p>
</div>

<!--/* 结合其他条件使用 */-->
<th:block th:if="${pluginFinder.available('extra-api') and extraApiPluginInfoFinder.isFullVersion()}">
    <!-- 只有在插件可用且为全量版时才显示 -->
    <div th:utext="${extraApiRenderFinder.renderCodeHtml(content)}"></div>
</th:block>
```

### 统计信息 API

**Finder 名称：** `extraApiStatsFinder`

#### 文章字数统计 API

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

#### 代码高亮 API

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

## 下载和安装

### 稳定版

稳定版通过 GitHub Releases 发布，建议生产环境使用。

1. 访问 [Releases 页面](https://github.com/HowieHz/halo-plugin-extra-api/releases)
2. 下载最新版本的 JAR 文件：
    - `extra-api-lite-版本号.jar`：轻量版（适用于所有平台）
    - `extra-api-full-all-platforms-版本号.jar`：全量版（包含所有平台依赖，体积较大，不推荐下载）
    - 如需使用全量版，推荐下载平台特定版本：
        - `extra-api-full-linux-arm64-版本号.jar`：适用于 Linux ARM64 平台的版本
        - `extra-api-full-linux-x86_64-版本号.jar`：适用于 Linux x86_64 平台的版本
        - `extra-api-full-macos-arm64-版本号.jar`：适用于 macOS ARM64 平台的版本
        - `extra-api-full-macos-x86_64-版本号.jar`：适用于 macOS x86_64 平台的版本
        - `extra-api-full-windows-x86_64-版本号.jar`：适用于 Windows x86_64 平台的版本
3. 将下载的 JAR 文件上传到 Halo 的插件管理页面安装

### 开发版

插件的开发版 JAR 文件通过 GitHub Actions 自动构建，仅用于测试和开发。

- [GitHub Actions Workflow](https://github.com/HowieHz/halo-plugin-extra-api/actions/workflows/workflow.yaml)

#### 下载步骤

1. 访问上述链接，选择最新的成功运行的 workflow。
2. 在 "Artifacts" 部分，下载 `extra-api` 压缩包。
3. 解压后，您将找到以下 JAR 文件：
    - `extra-api-lite-版本号-SNAPSHOT.jar`：轻量版（适用于所有平台）
    - `extra-api-full-all-platforms-版本号-SNAPSHOT.jar`：全量版（包含所有平台依赖）
    - `extra-api-full-linux-arm64-版本号-SNAPSHOT.jar`：全量版（适用于 Linux ARM64 平台）
    - `extra-api-full-linux-x86_64-版本号-SNAPSHOT.jar`：全量版（适用于 Linux x86_64 平台）
    - `extra-api-full-macos-arm64-版本号-SNAPSHOT.jar`：全量版（适用于 macOS ARM64 平台）
    - `extra-api-full-macos-x86_64-版本号-SNAPSHOT.jar`：全量版（适用于 macOS x86_64 平台）
    - `extra-api-full-windows-x86_64-版本号-SNAPSHOT.jar`：全量版（适用于 Windows x86_64 平台）

选择适合您系统的 JAR 文件安装到 Halo。

## 开发指南/贡献指南

参见 [CONTRIBUTING.md](./CONTRIBUTING.md)

## 许可证

[AGPL-3.0](./LICENSE) © HowieHz
