package top.howiehz.halo.plugin.extra.api.service.js.module;

/**
 * Types of JS modules supported by the resolver: UMD, ESM, CJS.
 * 模块类型：UMD（通用）、ESM（ES 模块）、CJS（CommonJS）。
 */
public enum JsModuleType {
    UMD,  // Universal Module Definition
    ESM,  // ES Module
    CJS   // CommonJS
}
