import { bundledLanguages, bundledThemes, createHighlighterCoreSync, createOnigurumaEngine } from 'shiki'

// 默认配置常量
const DEFAULT_LANG = 'javascript'
const DEFAULT_THEME = 'nord'

// 全局缓存的高亮器实例
let highlighter = null

// 全局缓存的引擎实例
let engineInstance = null

// 初始化 Promise 缓存,防止竞态条件
let initPromise = null

/**
 * 获取支持的语言列表
 */
function getSupportedLanguages() {
  return Object.keys(bundledLanguages)
}

/**
 * 获取支持的主题列表
 */
function getSupportedThemes() {
  return Object.keys(bundledThemes)
}

/**
 * 获取或创建高亮器实例（单例模式）
 * 一次性加载所有语言和主题,避免后续按需加载的开销
 * 使用 Promise 缓存避免竞态条件
 * @returns {Promise<void>}
 * @throws {Error} 初始化失败时抛出错误
 * 参考: https://shiki.zhcndoc.com/guide/best-performance
 */
async function initHighlighter() {
  // 如果已经在初始化中,返回缓存的 Promise
  if (initPromise) {
    return initPromise
  }

  // 如果已经初始化完成,直接返回
  if (highlighter && engineInstance) {
    return Promise.resolve()
  }

  // 创建初始化 Promise 并缓存
  initPromise = (async () => {
    try {
      if (!engineInstance) {
        engineInstance = await createOnigurumaEngine(import('shiki/wasm'))
      }

      if (!highlighter) {
        const langs = await Promise.all(
          Object.values(bundledLanguages).map(fn => fn())
        )
        const themes = await Promise.all(
          Object.values(bundledThemes).map(fn => fn())
        )

        highlighter = createHighlighterCoreSync({
          themes,
          langs,
          engine: engineInstance
        })
      }
    } catch (error) {
      // 初始化失败时清理状态,允许重试
      initPromise = null
      throw new Error(`高亮器初始化失败: ${error.message}`)
    }
  })()

  return initPromise
}

/**
 * 单个代码高亮 - 使用缓存的高亮器实例
 * @param {string} code - 要高亮的代码
 * @param {Object} options - 高亮选项
 * @param {string} [options.lang='javascript'] - 语言类型
 * @param {string} [options.theme='nord'] - 主题名称
 * @returns {Promise<string>} 高亮后的 HTML 字符串
 * @throws {Error} 高亮失败时抛出错误
 */
async function highlightCode(code, options = {}) {
  // 参数验证
  if (typeof code !== 'string') {
    throw new Error('代码参数必须是字符串')
  }

  try {
    await initHighlighter()

    const lang = options.lang || DEFAULT_LANG
    const theme = options.theme || DEFAULT_THEME

    // 验证语言和主题是否支持
    const supportedLangs = getSupportedLanguages()
    const supportedThemes = getSupportedThemes()

    if (!supportedLangs.includes(lang)) {
      throw new Error(`不支持的语言: ${lang}`)
    }

    if (!supportedThemes.includes(theme)) {
      throw new Error(`不支持的主题: ${theme}`)
    }

    const html = highlighter.codeToHtml(code, {
      lang,
      theme,
      ...options
    })

    return html
  } catch (error) {
    throw new Error(`代码高亮失败: ${error.message}`)
  }
}

/**
 * 批量高亮 - 在同一个高亮器实例中处理多个代码块
 * @param {Object<string, {code: string, lang?: string, theme?: string}>} requests - 批量请求对象
 * @returns {Promise<Object<string, {success: boolean, html?: string, error?: string}>>} 批量结果对象
 * @throws {Error} 批量处理失败时抛出错误
 */
async function highlightCodeBatch(requests) {
  // 参数验证
  if (!requests || typeof requests !== 'object') {
    throw new Error('请求参数必须是对象')
  }

  try {
    await initHighlighter()

    const results = {}

    // 并行处理所有代码高亮请求
    const resolvedResults = await Promise.all(
      Object.entries(requests).map(async ([id, request]) => {
        try {
          // 验证单个请求
          if (!request || typeof request.code !== 'string') {
            return [id, {
              success: false,
              error: '无效的请求格式: code 必须是字符串'
            }]
          }

          const lang = request.lang || DEFAULT_LANG
          const theme = request.theme || DEFAULT_THEME

          const html = highlighter.codeToHtml(request.code, {
            lang,
            theme
          })

          return [id, {
            success: true,
            html
          }]
        } catch (error) {
          return [id, {
            success: false,
            error: error.message
          }]
        }
      })
    )

    for (const [id, result] of resolvedResults) {
      results[id] = result
    }

    return results
  } catch (error) {
    throw new Error(`批量高亮失败: ${error.message}`)
  }
}

/**
 * 预热高亮器 - 提前初始化以提升首次调用性能
 * @returns {Promise<void>}
 * @throws {Error} 预热失败时抛出错误
 */
async function warmup() {
  await initHighlighter()
}

/**
 * 清理高亮器实例（用于引擎销毁时）
 * @returns {Promise<void>}
 */
async function disposeHighlighter() {
  if (highlighter) {
    highlighter.dispose()
    highlighter = null
  }

  // 清理引擎实例和初始化 Promise
  engineInstance = null
  initPromise = null
}

// 暴露给 globalThis
globalThis.highlightCode = highlightCode
globalThis.highlightCodeBatch = highlightCodeBatch
globalThis.getSupportedLanguages = getSupportedLanguages
globalThis.getSupportedThemes = getSupportedThemes
globalThis.warmup = warmup
globalThis.disposeHighlighter = disposeHighlighter

// 导出
export {
  highlightCode,
  highlightCodeBatch,
  getSupportedLanguages,
  getSupportedThemes,
  warmup,
  disposeHighlighter
}
