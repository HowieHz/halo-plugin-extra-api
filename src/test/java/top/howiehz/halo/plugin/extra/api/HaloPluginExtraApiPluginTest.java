package top.howiehz.halo.plugin.extra.api;

import com.caoccao.javet.exceptions.JavetException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import run.halo.app.plugin.PluginContext;
import top.howiehz.halo.plugin.extra.api.service.basic.post.stats.PostWordCountService;
import top.howiehz.halo.plugin.extra.api.service.js.engine.V8EnginePoolService;
import top.howiehz.halo.plugin.extra.api.service.js.adapters.shiki.ShikiHighlightService;

import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class HaloPluginExtraApiPluginTest {

    @Mock
    PluginContext context;

    @Mock
    PostWordCountService postWordCountService;

    @Mock
    V8EnginePoolService enginePoolService;

    @Mock
    ShikiHighlightService shikiHighlightService;

    @InjectMocks
    HaloPluginExtraApiPlugin plugin;

    @Test
    void contextLoads() throws JavetException {
        // 执行测试
        plugin.start();
        plugin.stop();
    }
}