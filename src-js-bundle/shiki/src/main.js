import { codeToHtml, bundledLanguages, bundledThemes } from 'shiki/bundle/full'

// 单个代码高亮 - 简单包装
async function highlightCode(code, options = {}) {
  try {
    const html = await codeToHtml(code, options)
    return html
  } catch (error) {
    throw new Error('高亮失败: ' + error.message)
  }
}

// 批量高亮 - 在一个引擎中处理多个代码块
async function highlightCodeBatch(requests) {
  try {
    const results = {}
    
    // 使用 Promise.all 在同一个引擎中并行处理
    const entries = Object.entries(requests)
    const promises = entries.map(async ([id, request]) => {
      try {
        const html = await codeToHtml(request.code, {
          lang: request.lang,
          theme: request.theme
        })
        return [id, html]
      } catch (error) {
        return [id, `Error: ${error.message}`]
      }
    })
    
    const resolvedResults = await Promise.all(promises)
    
    for (const [id, html] of resolvedResults) {
      results[id] = html
    }
    
    return results
  } catch (error) {
    throw new Error('批量高亮失败: ' + error.message)
  }
}

// 获取支持的语言列表
function getSupportedLanguages() {
  return Object.keys(bundledLanguages)
}

// 获取支持的主题列表  
function getSupportedThemes() {
  return Object.keys(bundledThemes)
}

// 暴露给 globalThis
globalThis.highlightCode = highlightCode
globalThis.highlightCodeBatch = highlightCodeBatch
globalThis.getSupportedLanguages = getSupportedLanguages
globalThis.getSupportedThemes = getSupportedThemes

// 导出
export { highlightCode, highlightCodeBatch, getSupportedLanguages, getSupportedThemes }
