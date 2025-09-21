package top.howiehz.halo.plugin.extra.api.utils;

import java.util.regex.Pattern;

/**
 * Utilities for counting words from HTML or plain text.
 * 从 HTML 或纯文本统计字数的工具类。
 */
public class PostWordCountUtil {

    // Patterns for stripping HTML quickly
    // 快速移除 HTML 的正则表达式
    private static final Pattern HTML_CONTENT_REMOVAL = Pattern.compile(
        "(?is)<(?:script|style)\\b[^>]*>.*?</(?:script|style)>|<[^>]+>|&[a-zA-Z0-9#]+;");

    /**
     * Extract plain text from HTML content by removing tags and entities.
     * Removes script and style tags, HTML tags, and normalizes whitespace.
     * 从 HTML 内容中提取纯文本，移除标签和实体。
     * 移除 script 和 style 标签、HTML 标签，并规范化空白字符。
     *
     * @param html the HTML content / HTML 内容
     * @return plain text / 返回纯文本
     */
    static String extractText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }

        // 一次性处理所有HTML标签和实体
        return HTML_CONTENT_REMOVAL.matcher(html).replaceAll(" ");
    }

    /**
     * Check if a Unicode code point belongs to CJK (Chinese, Japanese, Korean) character blocks.
     * Includes various CJK unified ideographs, compatibility ideographs, and phonetic extensions.
     * 检查 Unicode 码点是否属于中日韩 (CJK) 字符块。
     * 包括各种 CJK 统一表意文字、兼容表意文字和音标扩展。
     * Optimized CJK character detection using range checks.
     * 使用范围检查优化的CJK字符检测。
     */
    private static boolean isCJK(int codePoint) {
        // 常见CJK范围的快速检查 / Fast check for common CJK ranges
        return (codePoint >= 0x4E00 && codePoint <= 0x9FFF) ||     // CJK Unified Ideographs
            (codePoint >= 0x3400 && codePoint <= 0x4DBF) ||     // CJK Extension A
            (codePoint >= 0x20000 && codePoint <= 0x2A6DF) ||   // CJK Extension B
            (codePoint >= 0x2A700 && codePoint <= 0x2B73F) ||   // CJK Extension C
            (codePoint >= 0x2B740 && codePoint <= 0x2B81F) ||   // CJK Extension D
            (codePoint >= 0x2B820 && codePoint <= 0x2CEAF) ||   // CJK Extension E
            (codePoint >= 0x2CEB0 && codePoint <= 0x2EBEF) ||   // CJK Extension F
            (codePoint >= 0xF900 && codePoint <= 0xFAFF) ||     // CJK Compatibility Ideographs
            (codePoint >= 0x2F800 && codePoint <= 0x2FA1F) ||
            // CJK Compatibility Ideographs Supplement
            (codePoint >= 0x3040 && codePoint <= 0x309F) ||     // Hiragana
            (codePoint >= 0x30A0 && codePoint <= 0x30FF) ||     // Katakana
            (codePoint >= 0x31F0 && codePoint <= 0x31FF) ||     // Katakana Phonetic Extensions
            (codePoint >= 0xAC00 && codePoint <= 0xD7AF) ||     // Hangul Syllables
            (codePoint >= 0x1100 && codePoint <= 0x11FF) ||     // Hangul Jamo
            (codePoint >= 0x3130 && codePoint <= 0x318F);       // Hangul Compatibility Jamo
    }

    /**
     * Count words in text, supporting both CJK characters and ASCII words.
     * CJK characters are counted individually, ASCII letters/digits are grouped as words.
     * 统计文本中的词数，支持中日韩字符和 ASCII 单词。
     * 中日韩字符单独计数，ASCII 字母/数字按单词分组计数。
     *
     * @param text the input text / 输入文本
     * @return word count / 返回词数统计
     */
    public static int countPlainTextWords(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int count = 0; // 字数计数器 / Word count counter
        boolean inAsciiWord = false; // 是否在ASCII单词中 / Whether in an ASCII word
        int length = text.length();
        for (int i = 0; i < length; ) {
            int codePoint = text.codePointAt(i); // 获取当前字符的码点 / Get code point of current character
            if (isCJK(codePoint)) {
                // count each CJK code point as one word/char
                // 每个CJK码点计为一个字/词 / Count each CJK code point as one word/char
                count++;
                inAsciiWord = false; // 重置ASCII单词状态 / Reset ASCII word state
            } else if (Character.isLetterOrDigit(codePoint)) {
                // group consecutive ASCII letters/digits as one word
                // 连续的ASCII字母/数字作为一个单词 / Group consecutive ASCII letters/digits as one word
                if (!inAsciiWord) {
                    count++; // 开始新的ASCII单词 / Start a new ASCII word
                    inAsciiWord = true; // 设定在ASCII单词中 / Set in ASCII word
                }
            } else {
                inAsciiWord = false; // 非字母数字字符，重置状态 / Non-alphanumeric character, reset state
            }
            // 使用位运算优化字符长度计算 / Optimize character length calculation with bitwise operations
            i += (codePoint <= 0xFFFF) ? 1 : 2;
        }
        return Math.max(count, 0); // 确保返回非负数 / Ensure non-negative result
    }

    /**
     * Count words from raw HTML by stripping tags then counting plain text.
     * 从原始 HTML 中移除标签后统计文本词数。
     *
     * @param rawHtml raw HTML string / 原始 HTML 字符串
     * @return word count / 返回词数统计
     */
    public static int countHTMLWords(String rawHtml) {
        return countPlainTextWords(extractText(rawHtml));
    }
}
