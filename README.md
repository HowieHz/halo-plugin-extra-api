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
        th:text="|总字数：${extraApi.wordCount()}|"
    ></span>
</th:block>

<!--/* 写在一个标签内也可以，th:if 的优先级比 th:text 高 */-->
<span
    th:if="${pluginFinder.available('extra-api')}"
    th:text="|总字数：${extraApi.wordCount()}|"
></span>

<!--/* 自然模板写法 */-->
<span th:if="${pluginFinder.available('extra-api')}">总字数：[[${extraApi.wordCount()}]]</span>
```

**说明**

使用 `pluginFinder.available('extra-api')` 可以优雅地处理插件依赖，避免在插件未安装时出现模板错误，提升主题的兼容性和用户体验。

### 文章字数统计 API

**Finder 名称：** `extraApi`

#### 文章字数统计

```javascript
extraApi.wordCount({
  name: 'post-metadata-name',  // 可选，未传入则统计全部文章字数总和
  version: 'release' | 'draft'  // 可选，默认 'release'
});
```

```javascript
extraApi.wordCount();
```

**描述**

- 传入 name（`metadata.name`）可统计该文章的字数，配合 version 指定统计发布版本还是草稿版本。
- 当未传入 name 时，将统计全部文章的字数总和（同时依据 version 区分发布/草稿）。

- **计数规则**：
  - 中文、日文、韩文等 CJK 字符按每个字符计 1
  - ASCII 连续字母/数字按 1 个单词计数
  - 标点符号和空格不计入统计
- **错误处理**：输入为空或文章不存在时返回 0，不会抛出异常
- **性能说明**：
  - 单次调用开销较小，适合在模板中直接使用

**参数**
- `name:string` – 文章 `metadata.name`（可选，不传则统计全站）
- `version:string` – 统计版本，可选 `release`（默认）或 `draft`

**返回值**
- `int` – 字数统计结果（非负），不存在或参数缺失时返回 0

**使用示例**
```html
<!--/* 统计文章已发布版本的字，适用于 /templates/post.html */-->
<span th:text="${extraApi.wordCount({name: post.metadata.name})}"></span>

<!--/* 统计文章最新版本的字数（含草稿），适用于 /templates/post.html */-->
<span th:text="${extraApi.wordCount({name: post.metadata.name, version: 'draft'})}"></span>

<!--/* 统计全站已发布文章的总字数，适用于全部模板 */-->
<span th:text="${extraApi.wordCount()}"></span>
<!--/* 与下方写法等价 */-->
 <span th:text="${extraApi.wordCount({})}"></span>

<!--/* 统计全站所有文章最新版本的总字数（含草稿），适用于全部模板 */-->
<span th:text="${extraApi.wordCount({version: 'draft'})}"></span>
```

### 统一文章列表查询

Finder 名称：`extraApi`

方法签名：
```
extraApi.list({
  page: 1,
  size: 10,
  tagName: 'fake-tag',
  categoryName: 'fake-category',
  ownerName: 'fake-owner',
  sort: {'spec.publishTime,desc', 'metadata.creationTimestamp,asc'}
});
```

**描述**

统一参数的文章列表查询方法，支持分页、标签、分类、创建者、排序等参数，且均为可选参数。

**参数**
- `page:int` – 分页页码，从 1 开始，默认 1
- `size:int` – 分页条数，默认 10
- `tagName:string` – 标签唯一标识 `metadata.name`
- `categoryName:string` – 分类唯一标识 `metadata.name`
- `ownerName:string` – 创建者用户名 `name`
- `sort:string[]` – 排序字段，格式为 `字段名,排序方式`，排序方式可选 `asc` 或 `desc`。例如 `spec.publishTime,desc`。在模板中传递时请使用 `{}` 形式表示数组：`{'spec.publishTime,desc','metadata.creationTimestamp,asc'}`。

**返回值**
- `Mono<Page<Post>>` – 与 Halo 内置 [`postFinder.list({...})`](https://docs.halo.run/developer-guide/theme/finder-apis/post#list) 一致，可直接用于模板遍历。

**使用示例**
```html
<!-- 基础分页 -->
<th:block th:with="page=${extraApi.list({page:1,size:5})}">
  <ul>
    <li th:each="post : ${page.items}" th:text="${post.spec.title}">标题</li>
  </ul>
</th:block>

<!-- 带筛选和排序 -->
<th:block th:with="page=${extraApi.list({
  page:1,
  size:10,
  tagName:'tech',
  categoryName:'java',
  ownerName:'admin',
  sort:{'spec.publishTime,desc','metadata.creationTimestamp,asc'}
})}">
  <ul>
    <li th:each="post : ${page.items}" th:text="${post.spec.title}">标题</li>
  </ul>
</th:block>
``` 

**兼容性与实现说明**
- 本方法优先委托给 Halo 内置 `postFinder.list(Map)` 实现，确保标签/分类/创建者等复杂筛选能力与官方行为保持一致。
- 如果运行环境中不可用（例如版本差异），会回退到基础实现：支持 `page/size/sort`，不保证 `tagName/categoryName/ownerName` 过滤生效。

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
