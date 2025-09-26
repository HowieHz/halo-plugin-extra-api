package top.howiehz.halo.plugin.extra.api;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.NodeRuntime;
import com.caoccao.javet.interop.V8Host;
import com.caoccao.javet.values.reference.V8ValueFunction;
import com.caoccao.javet.values.reference.V8ValueObject;
import com.caoccao.javet.values.reference.V8ValuePromise;
import org.springframework.stereotype.Component;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;
import top.howiehz.halo.plugin.extra.api.service.PostWordCountService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Plugin main class to manage the lifecycle of the plugin.
 * 插件主类，负责管理插件的生命周期。
 * <p>Only one main class extending {@link BasePlugin} is allowed per plugin.</p>
 * <p>每个插件只能有一个继承 {@link BasePlugin} 的主类。</p>
 */
@Component
public class HaloPluginExtraApiPlugin extends BasePlugin {

    private final PostWordCountService postWordCountService;
    private NodeRuntime nodeRuntime;

    public HaloPluginExtraApiPlugin(PluginContext pluginContext,
        PostWordCountService postWordCountService) {
        super(pluginContext);
        this.postWordCountService = postWordCountService;
    }

    /**
     * Called when the plugin is starting.
     * 插件启动时调用。
     */
    @Override
    public void start() {
        System.out.println("插件启动成功！");
        // Preload all caches when the plugin starts
        // 插件启动时预加载所有缓存
        // post word count cache / 文章字数缓存
        postWordCountService.warmUpAllCache();

        try (NodeRuntime nodeRuntime = V8Host.getNodeInstance().createV8Runtime()) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("js/shiki.umd.cjs")) {
                if (is == null) {
                    throw new IOException("Cannot find shiki.umd.cjs file.");
                }

                // Load the UMD module content into V8Runtime
                String jsCode = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                nodeRuntime.getExecutor(jsCode).executeVoid();
            } catch (JavetException | IOException e) {
                throw new RuntimeException(e);
            }

            // 2. 获取全局对象
            V8ValueObject global = nodeRuntime.getGlobalObject();

            // 3. 获取 highlightCode 函数
            var value  = global.get("highlightCode");
            if (!(value instanceof V8ValueFunction highlightCode)) {
                throw new IllegalStateException("highlightCode 不是函数，或未定义。");
            }

            Map<String, Object> options = new HashMap<>();
            options.put("lang", "javascript");
            options.put("theme", "github-light");

            // 4. 调用 highlightCode (返回 Promise)
            String code = "const a = 123;";
            V8ValuePromise promise = highlightCode.call(null, code, options);

            // 5. 等待 Promise 结算
            while (promise.isPending()) {
                nodeRuntime.await();
            }

            // 6. 读取结果
            if (promise.isFulfilled()) {
                String html = promise.getResultString();
                System.out.println("高亮结果: " + html);
            } else if (promise.isRejected()) {
                System.err.println("Promise rejected: " + promise.getResultString());
            }
        } catch (JavetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Called when the plugin is stopping.
     * 插件停止时调用。
     */
    @Override
    public void stop() {
        System.out.println("插件停止！");
    }
}
