import { defineConfig } from 'vite'

export default defineConfig({
  build: {
    outDir: 'build/dist',
    lib: {
      entry: 'src/main.js',
      fileName: 'shiki',
      name: 'shiki',
      formats: ['umd']
    },
    rollupOptions: {
      output: {
        // 禁用代码分割，确保单文件
        manualChunks: undefined,
        // 内联所有依赖
        inlineDynamicImports: true
      }
    }
  }
})
