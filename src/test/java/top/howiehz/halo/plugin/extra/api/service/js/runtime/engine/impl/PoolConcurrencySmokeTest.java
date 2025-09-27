package top.howiehz.halo.plugin.extra.api.service.js.runtime.engine.impl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class PoolConcurrencySmokeTest {

    @Test
    public void smokeTest() throws Exception {
        // 初始化池（不通过 Spring，上下文）
        V8EnginePoolServiceImpl svc = new V8EnginePoolServiceImpl();
        svc.afterPropertiesSet();

        int concurrentThreads = 4; // 提交任务数（可以大于 poolMaxSize 以观察排队）
        try (ExecutorService es = Executors.newFixedThreadPool(concurrentThreads)) {

            CountDownLatch latch = new CountDownLatch(concurrentThreads);
            for (int i = 0; i < concurrentThreads; i++) {
                final int idx = i;
                es.submit(() -> {
                    try {
                        svc.withEngine(runtime -> {
                            // 在 engine 上做一些真实工作（计算 + sleep 模拟耗时）
                            log.info("task {} running on thread {}", idx, Thread.currentThread()
                                .getName());
                            runtime.getExecutor("var s=0; for (var i=0;i<1e6;i++) s+=i; s")
                                .executeInteger();
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException ignored) {
                            }
                            return null;
                        });
                    } catch (Exception e) {
                        log.error("task {} failed", idx);
                        log.error("Full stack trace: ", e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // 等待任务完成或超时
            var ignore = latch.await(2, TimeUnit.MINUTES);
            es.shutdownNow();
        }

        // 释放资源
        svc.destroy();
    }
}

