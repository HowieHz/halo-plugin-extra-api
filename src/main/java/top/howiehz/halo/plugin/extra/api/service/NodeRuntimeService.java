package top.howiehz.halo.plugin.extra.api.service;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.NodeRuntime;
import com.caoccao.javet.interop.V8Host;
import com.caoccao.javet.values.V8Value;
import com.caoccao.javet.values.reference.IV8ValuePromise;
import com.caoccao.javet.values.reference.V8ValueFunction;
import com.caoccao.javet.values.reference.V8ValueObject;
import com.caoccao.javet.values.reference.V8ValuePromise;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class NodeRuntimeService implements InitializingBean, DisposableBean {

    private NodeRuntime nodeRuntime;
    private final ReentrantLock lock = new ReentrantLock();
    private volatile boolean initialized = false;

    @Override
    public void afterPropertiesSet() {
        try {
            nodeRuntime = V8Host.getNodeInstance().createV8Runtime();
            loadShikiBundle();
            initialized = true;
            System.out.println("NodeRuntime initialized successfully");
        } catch (Exception e) {
            System.err.println("NodeRuntime initialization failed: " + e.getMessage());
            e.fillInStackTrace();
        }
    }

    @Override
    public void destroy() {
        if (nodeRuntime != null) {
            try {
                nodeRuntime.close();
                System.out.println("NodeRuntime closed successfully");
            } catch (JavetException e) {
                System.err.println("Failed to close NodeRuntime: " + e.getMessage());
            }
        }
    }

    /**
     * 加载 Shiki bundle
     */
    private void loadShikiBundle() throws IOException, JavetException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("js/shiki.umd.cjs")) {
            if (is == null) {
                throw new IOException("Cannot find resource: js/shiki.umd.cjs");
            }
            String jsCode = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            nodeRuntime.getExecutor(jsCode).executeVoid();
        }
    }

    /**
     * 代码高亮（异步）
     */
    public CompletableFuture<String> highlightCodeAsync(String code, Map<String, Object> options) {
        if (!initialized) {
            return CompletableFuture.failedFuture(new IllegalStateException("NodeRuntime not initialized"));
        }

        CompletableFuture<String> future = new CompletableFuture<>();

        // 使用锁保证线程安全
        lock.lock();
        try {
            V8ValueObject global = nodeRuntime.getGlobalObject();
            var value = global.get("highlightCode");

            if (!(value instanceof V8ValueFunction highlightCode)) {
                future.completeExceptionally(new IllegalStateException("Not found highlightCode function"));
                return future;
            }

            try (V8ValuePromise promise = highlightCode.call(null, code, options)) {
                registerPromiseListener(promise, future);
                startEventLoop(future);
            }

        } catch (Exception e) {
            future.completeExceptionally(e);
        } finally {
            lock.unlock();
        }

        return future;
    }

    /**
     * 执行 JavaScript 代码（同步）
     */
    public <T> T executeScript(String script, Class<T> returnType) throws JavetException {
        if (!initialized) {
            throw new IllegalStateException("NodeRuntime not initialized");
        }

        lock.lock();
        try {
            Object result;
            if (returnType == String.class) {
                result = nodeRuntime.getExecutor(script).executeString();
            } else if (returnType == Integer.class) {
                result = nodeRuntime.getExecutor(script).executeInteger();
            } else if (returnType == Boolean.class) {
                result = nodeRuntime.getExecutor(script).executeBoolean();
            } else {
                result = nodeRuntime.getExecutor(script).executeObject();
            }
            return returnType.cast(result);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取 NodeRuntime（谨慎使用，需要调用者自行保证线程安全）
     */
    public NodeRuntime getNodeRuntime() {
        if (!initialized) {
            throw new IllegalStateException("NodeRuntime not initialized");
        }
        return nodeRuntime;
    }

    private void registerPromiseListener(V8ValuePromise promise, CompletableFuture<String> future) throws JavetException {

        boolean registered = promise.register(new IV8ValuePromise.IListener() {
            @Override
            public void onFulfilled(V8Value v8Value) {
                try {
                    future.complete(v8Value.toString());
                } catch (Exception e) {
                    future.completeExceptionally(e);
                } finally {
                    closeQuietly(v8Value);
                }
            }

            @Override
            public void onRejected(V8Value v8Value) {
                try {
                    future.completeExceptionally(new RuntimeException("Promise rejected: " + v8Value.toString()));
                } catch (Exception e) {
                    future.completeExceptionally(e);
                } finally {
                    closeQuietly(v8Value);
                }
            }

            @Override
            public void onCatch(V8Value v8Value) {
                try {
                    future.completeExceptionally(new RuntimeException("Promise caught: " + v8Value.toString()));
                } catch (Exception e) {
                    future.completeExceptionally(e);
                } finally {
                    closeQuietly(v8Value);
                }
            }
        });

        if (!registered) {
            if (promise.isFulfilled()) {
                future.complete(promise.getResultString());
            } else if (promise.isRejected()) {
                future.completeExceptionally(new RuntimeException("Promise rejected: " + promise.getResultString()));
            }
        }
    }

    private void startEventLoop(CompletableFuture<?> future) {
        Thread eventLoopThread = new Thread(() -> {
            try {
                while (!future.isDone()) {
                    lock.lock();
                    try {
                        nodeRuntime.await();
                    } finally {
                        lock.unlock();
                    }
                    Thread.sleep(10);
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }, "NodeRuntime-EventLoop");

        eventLoopThread.setDaemon(true);
        eventLoopThread.start();
    }

    private void closeQuietly(V8Value v8Value) {
        if (v8Value != null) {
            try {
                v8Value.close();
            } catch (Exception e) {
                System.err.println("Failed to close V8Value: " + e.getMessage());
            }
        }
    }
}