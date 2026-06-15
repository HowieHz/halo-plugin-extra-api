package top.howiehz.halo.plugin.extra.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import run.halo.app.plugin.PluginContext;
import top.howiehz.halo.plugin.extra.api.service.core.post.stats.PostWordCountService;

@ExtendWith(MockitoExtension.class)
class HaloPluginExtraApiPluginTest {

    @Mock
    PluginContext context;

    @Mock
    PostWordCountService postWordCountService;

    @InjectMocks
    HaloPluginExtraApiPlugin plugin;

    @Test
    void contextLoads() {
        // 执行测试
        plugin.start();
        plugin.stop();
    }
}
