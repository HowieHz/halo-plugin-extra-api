package top.howiehz.gradle.utils;

import org.objectweb.asm.Opcodes;

/**
 * ASM 常量定义
 */
public final class AsmConst {

    public static final int ASM_VERSION = Opcodes.ASM9;

    private AsmConst() {
        // 工具类，禁止实例化
    }
}
