# halo-plugin-extra-api

## 简介

一个为 Halo CMS 提供额外 API 的轻量级插件。

## 核心理念

> CMS 的价值就是在服务端管理和处理内容，为什么要把简单的数据处理推到前端去增加复杂度呢？

让 CMS 回归"内容即数据"的本质，减少**不必要**的前端异步请求和动态渲染。

这个插件正是基于这个理念：
- 让复杂的逻辑在后端处理
- 前端模板只负责展示
- 提供简洁的 Finder API
- 减少不必要的 JavaScript 依赖

<details><summary>前端动态加载方式 vs 后端服务端渲染方式</summary>

| 对比维度 | 前端动态加载方式 | 后端服务端渲染方式 |
|---------|-----------------|-------------------|
| **性能表现** | ❌ 需要额外 HTTP 请求，增加延迟 | ✅ 服务端渲染，一次性输出 |
| **用户体验** | ❌ 页面闪烁，先显示占位符后填充数据 | ✅ 内容立即可见，无加载状态 |
| **SEO 友好** | ❌ 搜索引擎难以抓取动态内容 | ✅ 服务端渲染，完全 SEO 友好 |
| **错误处理** | ❌ 需要处理网络失败、超时等异常 | ✅ 服务端统一异常处理，减轻主题作者心智负担 |
| **开发复杂度** | ❌ 需要编写 JS 代码、状态管理、DOM 操作 | ✅ 模板中直接调用，代码简洁 |
| **缓存策略** | ❌ 需要前端缓存逻辑或重复请求 | ✅ 可利用模板缓存和服务端缓存 |
| **首屏渲染 (FCP)** | ❌ 需要等待 JS 执行和 API 响应 | ✅ HTML 直接包含内容，渲染更快 |
| **最大内容绘制 (LCP)** | ❌ 动态内容加载延迟主要内容显示 | ✅ 关键内容随页面一起渲染 |
| **累积布局偏移 (CLS)** | ❌ 内容异步加载可能导致页面跳动 | ✅ 静态布局，无意外的布局变化 |
| **交互响应 (INP)** | ❌ JS 执行和 DOM 操作影响交互性能 | ✅ 减少 JS 负担，交互更流畅 |

</details>

## TODO

- [] 缓存文章字数统计 API 结果
- [] 提供随机文章 API
- [] 提供预计阅读时间 API，及相关配置项

## 本插件可用 API 列表

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
        th:text="|字数：${extraApi.releaseCountByName(post.metadata.name)}|"
    ></span>
</th:block>

<!--/* 写在一个标签内也可以，th:if 的优先级比 th:text 高 */-->
<span
    th:if="${pluginFinder.available('extra-api')}"
    th:text="|字数：${extraApi.releaseCountByName(post.metadata.name)}|"
></span>

<!--/* 自然模板写法 */-->
<span th:if="${pluginFinder.available('extra-api')}">字数：[[${extraApi.releaseCountByName(post.metadata.name)}]]</span>
```

**说明**

使用 `pluginFinder.available('extra-api')` 可以优雅地处理插件依赖，避免在插件未安装时出现模板错误，提升主题的兼容性和用户体验。

### 文章字数统计

本插件提供了一个 API 用于查询文章字数，可查询指定文章/全部文章总和。

### 文章字数统计 Finder API

**Finder 名称：** `extraApi`

#### releaseCountByName(name)

```
extraApi.releaseCountByName(name);
```

**描述**

根据文章的 metadata.name 获取已发布内容的字数统计。

**参数**
- `name:string` - 文章的唯一标识 metadata.name。

**返回值**
- `int` - 字数统计结果，如果文章不存在或输入为空则返回 0。

**示例**
```html
<div th:with="post = ${postFinder.getByName('post-foo')}">
  <span>字数：</span>
  <span th:text="${extraApi.releaseCountByName(post.metadata.name)}">0</span>
</div>
```

#### headCountByName(name)

```
extraApi.headCountByName(name);
```

**描述**

根据文章的 `metadata.name` 获取最新 HEAD 内容（包含草稿）的字数统计。

**参数**
- `name:string` - 文章的唯一标识 `metadata.name`。

**返回值**
- `int` - 字数统计结果，如果文章不存在或输入为空则返回 0。

**示例**
```html
<div th:with="post = ${postFinder.getByName('post-foo')}">
  <span>当前内容字数：</span>
  <span th:text="${extraApi.headCountByName(post.metadata.name)}">0</span>
</div>
```

#### releaseCountBySlug(slug)

```
extraApi.releaseCountBySlug(slug);
```

**描述**

根据文章的 spec.slug 获取已发布内容的字数统计。

**参数**
- `slug:string` - 文章的别名 `spec.slug`。

**返回值**
- `int` - 字数统计结果，如果文章不存在或输入为空则返回 0。

**示例**
```html
<div>
  <span>字数：</span>
  <span th:text="${extraApi.releaseCountBySlug('hello-world')}">0</span>
</div>
```

#### headCountBySlug(slug)

```
extraApi.headCountBySlug(slug);
```

**描述**

根据文章的 spec.slug 获取最新 HEAD 内容（包含草稿）的字数统计。

**参数**
- `slug:string` - 文章的别名 spec.slug。

**返回值**
- `int` - 字数统计结果，如果文章不存在或输入为空则返回 0。

**示例**
```html
<div>
  <span>当前内容字数：</span>
  <span th:text="${extraApi.headCountBySlug('hello-world')}">0</span>
</div>
```

#### 说明

- **发布（release）版本**：只统计已发布的内容
- **HEAD 版本**：包含还未发布的草稿内容
- **计数规则**：
  - 中文、日文、韩文等 CJK 字符按每个字符计 1
  - ASCII 连续字母/数字按 1 个单词计数
  - 标点符号和空格不计入统计
- **错误处理**：输入为空或文章不存在时返回 0，不会抛出异常
- **性能说明**：
  - 单次调用开销较小，适合在模板中直接使用
  - 通过 spec.slug 查找文章依赖 Halo 内置索引，无需额外配置
  - 需要 Halo 版本 2.21 及以上。

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
