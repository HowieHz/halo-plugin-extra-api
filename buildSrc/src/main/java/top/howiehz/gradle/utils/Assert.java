package top.howiehz.gradle.utils;

/**
 * 简单的断言工具类
 */
public final class Assert {

    private Assert() {
        // 工具类，禁止实例化
    }

    /**
     * 断言对象不为 null
     *
     * @param object  要检查的对象
     * @param message 错误消息
     * @throws IllegalArgumentException 如果对象为 null
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
