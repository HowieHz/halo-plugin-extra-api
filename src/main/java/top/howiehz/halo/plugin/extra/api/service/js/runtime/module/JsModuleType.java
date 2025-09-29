package top.howiehz.halo.plugin.extra.api.service.js.runtime.module;

/**
 * Types of JS modules supported by the resolver: UMD, ESM, CJS.
 * 模块解析器支持的 JS 模块类型：UMD、ESM、CJS。
 */
public enum JsModuleType {
    /**
     * Universal Module Definition - works in browsers and Node.js.
     * 通用模块定义 - 在浏览器和 Node.js 中都能工作。
     */
    UMD,

    /**
     * ES Module - modern JavaScript module format.
     * ES 模块 - 现代 JavaScript 模块格式。
     */
    ESM,

    /**
     * CommonJS - traditional Node.js module format.
     * CommonJS - 传统的 Node.js 模块格式。
     */
    CJS
}
