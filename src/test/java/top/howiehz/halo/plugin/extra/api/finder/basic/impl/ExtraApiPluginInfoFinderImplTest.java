package top.howiehz.halo.plugin.extra.api.finder.basic.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Plugin Version Finder Test - 插件版本检测器测试
 * 测试在完整版环境下的行为（因为测试时 V8EnginePoolService 类存在）
 */
class ExtraApiPluginInfoFinderImplTest {

    private final ExtraApiPluginInfoFinderImpl finder = new ExtraApiPluginInfoFinderImpl();

    @Test
    void shouldDetectFullVersionWhenV8ServiceExists() {
        // 在测试环境中，V8EnginePoolService 类应该存在，所以应该检测为完整版
        Boolean result = finder.isFullVersion().block();
        assertTrue(result, "Should detect full version when V8EnginePoolService class exists");
    }

    @Test
    void shouldDetectNotLiteVersionWhenV8ServiceExists() {
        Boolean result = finder.isLiteVersion().block();
        assertFalse(result, "Should not detect lite version when V8EnginePoolService class exists");
    }

    @Test
    void shouldReturnFullVersionType() {
        String result = finder.getVersionType().block();
        assertEquals("full", result, "Should return 'full' as version type");
    }

    @Test
    void shouldDetectJavaScriptAvailable() {
        Boolean result = finder.isJavaScriptAvailable().block();
        assertTrue(result, "JavaScript should be available in full version");
    }

    @Test
    void shouldCacheResults() {
        // 第一次调用
        Boolean firstResult = finder.isFullVersion().block();
        assertTrue(firstResult);

        // 第二次调用应该使用缓存结果
        Boolean secondResult = finder.isFullVersion().block();
        assertTrue(secondResult);
        assertEquals(firstResult, secondResult, "Results should be consistent (cached)");
    }
}
