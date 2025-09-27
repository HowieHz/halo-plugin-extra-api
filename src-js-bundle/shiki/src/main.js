import { codeToHtml, bundledLanguages, bundledThemes } from 'shiki/bundle/full'

// 创建一个包装函数给 Javet 调用
async function highlightCode(code, options = {}) {
  try {
    const html = await codeToHtml(code, options)
    return html
  } catch (error) {
    throw new Error('高亮失败: ' + error.message)
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
globalThis.getSupportedLanguages = getSupportedLanguages
globalThis.getSupportedThemes = getSupportedThemes

// 导出
export { highlightCode, getSupportedLanguages, getSupportedThemes }