import browserslist from "browserslist";
import { browserslistToTargets } from "lightningcss";
import { defineConfig, type DefaultTheme } from "vitepress";
import { chineseSearchOptimize, pagefindPlugin } from "vitepress-plugin-pagefind";

const baseUrl = "https://howiehz.top";
const basePath = "/halo-plugins/";
const githubRepoUrl = "https://github.com/HowieHz/halo-plugins";

export default defineConfig({
  title: "Halo Plugins",
  titleTemplate: ":title | Halo Plugins",
  description: "HowieHz 的 Halo CMS 插件集合文档，包含 Extra API、Node.js 运行时、HTML 页面压缩和公共运行时 API。",
  lang: "zh-Hans",

  base: basePath,
  lastUpdated: true,
  cleanUrls: true,
  metaChunk: true,

  vite: {
    plugins: [
      pagefindPlugin({
        customSearchQuery: chineseSearchOptimize,
        locales: {
          root: {
            btnPlaceholder: "搜索",
            placeholder: "搜索文档",
            emptyText: "空空如也",
            heading: "共 {{searchResult}} 条结果",
            toSelect: "选择",
            toNavigate: "切换",
            toClose: "关闭",
            searchBy: "搜索提供者",
          },
        },
      }),
    ],
    css: {
      transformer: "lightningcss",
      lightningcss: {
        targets: browserslistToTargets(
          browserslist(["defaults", "Chrome >= 111", "Edge >= 111", "Firefox >= 114", "Safari >= 16.4"]),
        ),
      },
    },
    build: {
      cssMinify: "lightningcss",
    },
  },

  head: [
    ["meta", { name: "theme-color", content: "#2563eb" }],
    ["meta", { property: "og:type", content: "website" }],
    ["meta", { property: "og:site_name", content: "Halo Plugins" }],
    ["meta", { property: "og:image", content: `${baseUrl}${basePath}extra-api-logo.png` }],
    ["meta", { property: "og:url", content: `${baseUrl}${basePath}` }],
    [
      "script",
      {
        defer: "",
        src: "https://um.howiehz.top/script.js",
        "data-website-id": "7b461ac5-155d-45a8-a118-178d0a2936e4",
        "data-domains": "howiehz.top",
        "data-performance": "true",
      },
    ],
  ],

  sitemap: {
    hostname: `${baseUrl}${basePath}`,
  },

  markdown: {
    image: {
      lazyLoading: true,
    },
    container: {
      tipLabel: "提示",
      warningLabel: "警告",
      dangerLabel: "危险",
      infoLabel: "信息",
      detailsLabel: "详细信息",
    },
  },

  themeConfig: {
    logo: { src: "/extra-api-logo.png", width: 24, height: 24 },
    nav: nav(),
    sidebar: sidebar(),
    socialLinks: [{ icon: "github", link: githubRepoUrl }],

    docFooter: {
      prev: "上一页",
      next: "下一页",
    },

    footer: {
      message: "基于 AGPL-3.0 许可发布",
      copyright: "版权所有 © 2024-至今 HowieHz",
    },

    editLink: {
      pattern: `${githubRepoUrl}/edit/main/docs/:path`,
      text: "在 GitHub 上编辑此页面",
    },

    outline: {
      label: "本页大纲",
    },

    lastUpdated: {
      text: "最后更新于",
    },

    notFound: {
      title: "页面未找到",
      quote: "这个页面不在当前插件文档里。",
      linkLabel: "前往首页",
      linkText: "回到文档首页",
    },

    returnToTopLabel: "回到顶部",
    sidebarMenuLabel: "菜单",
    darkModeSwitchLabel: "主题",
    lightModeSwitchTitle: "切换到浅色模式",
    darkModeSwitchTitle: "切换到深色模式",
    skipToContentLabel: "跳转到内容",
  },
});

function nav(): DefaultTheme.NavItem[] {
  return [
    {
      text: "指南",
      items: [
        { text: "快速开始", link: "/guide/getting-started" },
        { text: "包与制品", link: "/guide/packages" },
        { text: "主题接入", link: "/guide/theme-integration" },
      ],
    },
    {
      text: "参考",
      items: [
        { text: "Extra API", link: "/reference/extra-api" },
        { text: "Node.js 运行时", link: "/reference/nodejs-runtime" },
        { text: "HTML 页面压缩", link: "/reference/minify-html" },
        { text: "Node Runtime API", link: "/reference/nodejs-runtime-api" },
        { text: "Node 运行时设计", link: "/node-runtime-design" },
      ],
    },
    {
      text: "维护",
      items: [
        { text: "开发", link: "/maintenance/development" },
        { text: "发布流程", link: "/maintenance/releases" },
      ],
    },
    { text: "GitHub", link: githubRepoUrl },
  ];
}

function sidebar(): DefaultTheme.SidebarItem[] {
  return [
    {
      text: "指南",
      items: [
        { text: "快速开始", link: "/guide/getting-started" },
        { text: "包与制品", link: "/guide/packages" },
        { text: "主题接入", link: "/guide/theme-integration" },
      ],
    },
    {
      text: "插件参考",
      items: [
        { text: "Extra API", link: "/reference/extra-api" },
        { text: "Node.js 运行时", link: "/reference/nodejs-runtime" },
        { text: "HTML 页面压缩", link: "/reference/minify-html" },
        { text: "Node Runtime API", link: "/reference/nodejs-runtime-api" },
      ],
    },
    {
      text: "维护",
      items: [
        { text: "开发", link: "/maintenance/development" },
        { text: "发布流程", link: "/maintenance/releases" },
        { text: "Node 运行时设计", link: "/node-runtime-design" },
      ],
    },
  ];
}
