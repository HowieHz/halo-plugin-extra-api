package top.howiehz.halo.plugin.extra.api.finder;

import java.util.*;
import java.util.regex.Pattern;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.content.ContentWrapper;
import run.halo.app.content.PostContentService;
import run.halo.app.core.extension.content.Post;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.theme.finders.Finder;

/**
 * Implementation of ExtraApiFinder.
 */
@Component
@Finder("extraApi")
public class ExtraApiFinderImpl implements ExtraApiFinder {

    private final ReactiveExtensionClient client; // 响应式扩展客户端 / Reactive extension client
    private final PostContentService postContentService; // 文章内容服务 / Post content service

    /**
     * Constructor to initialize ExtraApiFinderImpl with required dependencies.
     * 构造函数，使用必需的依赖项初始化 ExtraApiFinderImpl。
     */
    public ExtraApiFinderImpl(ReactiveExtensionClient client,
        PostContentService postContentService) {
        this.client = client; // 注入响应式扩展客户端 / Inject reactive extension client
        this.postContentService = postContentService; // 注入文章内容服务 / Inject post content service
    }

    /**
     * Get the word count of content by post name and method name.
     * 根据文章名称和方法名获取内容的字数统计。
     *
     * @param name the post name / 文章名称
     * @param methodName the method name to invoke / 要调用的方法名
     * @return word count as Mono / 返回字数统计的 Mono
     */
    private Mono<Integer> postContentCountByName(String name, String methodName) {
        if (name == null || name.isBlank()) {
            return Mono.just(0); // 空名称直接返回0 / Return 0 for empty name
        }

        // 使用函数接口映射方法名到对应的服务调用 / Use function interface to map method name to service call
        Mono<ContentWrapper> contentMono = switch (methodName) {
            case "getHeadContent" -> postContentService.getHeadContent(name);
            case "getReleaseContent" -> postContentService.getReleaseContent(name);
            default -> Mono.empty(); // 不支持的方法名返回空 / Return empty for unsupported method
        };

        return contentMono.map(ContentWrapper::getContent) // 提取 content 字段 / Extract content field
            .map(content -> countWords(
                extractText(content))) // 从 HTML 提取文本并计数 / Extract text and count
            .onErrorReturn(0) // 出错时返回 0 / Return 0 on error
            .defaultIfEmpty(0); // 空结果时返回 0 / Return 0 if empty
    }

    // Patterns for stripping HTML quickly
    // 快速移除HTML的正则表达式模式 / Patterns for quickly stripping HTML
    private static final Pattern HTML_CONTENT_REMOVAL =
        Pattern.compile("(?is)<(?:script|style)[^>]*>.*?</(?:script|style)>|<[^>]+>|&nbsp;");
    private static final Pattern WHITESPACE_NORMALIZATION = Pattern.compile("\\s+");
    private static final Pattern PUNCTUATION_SPACING = Pattern.compile("\\s+([,\\.!\\?:;，。！？：；、])");

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
        String text = HTML_CONTENT_REMOVAL.matcher(html).replaceAll(" ");

        // 批量替换特殊字符
        text = text.replace('\u00A0', ' ').replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');

        // 规范化空白字符
        text = WHITESPACE_NORMALIZATION.matcher(text).replaceAll(" ");

        // 处理标点符号前的空格
        text = PUNCTUATION_SPACING.matcher(text).replaceAll("$1");

        return text.trim();
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
    static int countWords(String text) {
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
                    inAsciiWord = true; // 设置在ASCII单词中 / Set in ASCII word
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
     * Unified word count API without slug support.
     * If name provided, count by name; otherwise sum word counts across all posts
     * (release/draft selectable by version).
     * 统一的字数统计API，不支持slug。
     * 如果提供name参数则统计指定文章，否则统计所有文章的字数总和。
     *
     * @param params parameter map: name? version? ('release'|'draft', default 'release')
     * @return word count as Mono (non-negative)
     */
    @Override
    public Mono<Integer> wordCount(Map<String, Object> params) {
        Map<String, Object> map = params == null ? java.util.Collections.emptyMap() : params;
        String name = String.valueOf(map.get("name"));
        boolean isDraft =
            String.valueOf(map.getOrDefault("version", "release")).equalsIgnoreCase("draft");

        if ("null".equals(name) || name.isBlank()) {
            return sumWordCountsAcrossAllPosts(isDraft);
        }

        return isDraft ? postContentCountByName(name, "getHeadContent")
            : postContentCountByName(name, "getReleaseContent");
    }

    /**
     * sum word counts across all posts with pagination.
     * 统计所有文章的字数总和。
     */
    private Mono<Integer> sumWordCountsAcrossAllPosts(boolean isDraft) {
        return client.listAll(Post.class, ListOptions.builder().build(), Sort.unsorted())
            .map(post -> post.getMetadata().getName()) // 提取需要的名称
            .flatMapSequential(postName -> isDraft ? postContentCountByName(postName, "getHeadContent")
                : postContentCountByName(postName, "getReleaseContent"), 128) // 128 并发
            .reduce(0, Integer::sum) // 直接累加
            .onErrorReturn(0);
    }
}
