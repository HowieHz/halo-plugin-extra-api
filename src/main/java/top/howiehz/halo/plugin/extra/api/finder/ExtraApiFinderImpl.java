package top.howiehz.halo.plugin.extra.api.finder;

import java.util.*;
import java.util.regex.Pattern;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.content.ContentWrapper;
import run.halo.app.content.PostContentService;
import run.halo.app.core.extension.content.Post;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.PageRequestImpl;
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

    // CJK 字符块集合 / CJK character block set
    private static final Set<Character.UnicodeBlock> CJK_BLOCKS = Set.of(
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_E,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_F,
        Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS,
        Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT,
        Character.UnicodeBlock.HIRAGANA,
        Character.UnicodeBlock.KATAKANA,
        Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS,
        Character.UnicodeBlock.HANGUL_SYLLABLES,
        Character.UnicodeBlock.HANGUL_JAMO,
        Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO
    );

    // Patterns for stripping HTML quickly
    // 快速移除HTML的正则表达式模式 / Patterns for quickly stripping HTML
    private static final Pattern SCRIPT = Pattern.compile("(?is)<script[^>]*>.*?</script>");
    private static final Pattern STYLE = Pattern.compile("(?is)<style[^>]*>.*?</style>");
    private static final Pattern TAG = Pattern.compile("(?s)<[^>]+>");
    private static final Pattern ENTITY_NBSP = Pattern.compile("&nbsp;", Pattern.CASE_INSENSITIVE);

    /**
     * Constructor to initialize ExtraApiFinderImpl with required dependencies.
     * 构造函数，使用必需的依赖项初始化 ExtraApiFinderImpl。
     */
    public ExtraApiFinderImpl(ReactiveExtensionClient client, PostContentService postContentService) {
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
    private Mono<Integer> contentCountByName(String name, String methodName) {
        if (name == null || name.isBlank()) {
            return Mono.just(0); // 空名称直接返回0 / Return 0 for empty name
        }

        // 根据方法名选择对应的内容获取方式 / Choose content retrieval method based on method name
        if ("getHeadContent".equals(methodName)) {
            // 获取最新版本内容（包括正在编辑的草稿） / Get latest version content (including drafts being edited)
            return postContentService.getHeadContent(name)
                .map(ContentWrapper::getContent) // 提取 content 字段 / Extract content field
                .map(content -> countWords(extractText(content))) // 提取文本并计数 / Extract text and count
                .onErrorReturn(0) // 出错时返回 0 / Return 0 on error
                .defaultIfEmpty(0); // 空结果时返回 0 / Return 0 if empty
        } else if ("getReleaseContent".equals(methodName)) {
            // 获取最新发布的文章内容 / Get latest published content
            return postContentService.getReleaseContent(name)
                .map(ContentWrapper::getContent) // 提取 content 字段 / Extract content field
                .map(content -> countWords(extractText(content))) // 提取文本并计数 / Extract text and count
                .onErrorReturn(0) // 出错时返回 0 / Return 0 on error
                .defaultIfEmpty(0); // 空结果时返回 0 / Return 0 if empty
        } else {
            // 不支持的方法名 / Unsupported method name
            return Mono.just(0);
        }
    }

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
        String text = SCRIPT.matcher(html).replaceAll(" "); // 移除script标签 / Remove script tags
        text = STYLE.matcher(text).replaceAll(" "); // 移除style标签 / Remove style tags
        text = TAG.matcher(text).replaceAll(" "); // 移除HTML标签 / Remove HTML tags
        text = ENTITY_NBSP.matcher(text).replaceAll(" "); // 替换&nbsp;实体 / Replace &nbsp; entity
        // Collapse whitespace / 规范化空白字符
        text = text.replace('\u00A0', ' '); // 替换不间断空格 / Replace non-breaking space
        text = text.replaceAll("[\\r\\n\\t]",
            " "); // 替换换行符和制表符为空格 / Replace newlines and tabs with space
        text = text.replaceAll("\\s+", " "); // 合并多个空格为一个 / Collapse multiple spaces to one
        // Remove space before common punctuation (ASCII and CJK) / 移除常见标点符号前的空格（ASCII和CJK）
        text = text.replaceAll("\\s+([,\\.!\\?:;])",
            "$1"); // 移除ASCII标点前的空格 / Remove space before ASCII punctuation
        text = text.replaceAll("\\s+([，。！？：；、])",
            "$1"); // 移除CJK标点前的空格 / Remove space before CJK punctuation
        return text.trim(); // 去除首尾空格 / Trim leading and trailing spaces
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
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i); // 获取当前字符的码点 / Get code point of current character
            int charCount = Character.charCount(cp); // 获取字符占用的字符单位数 / Get number of character units
            if (isCJK(cp)) {
                // count each CJK code point as one word/char
                // 每个CJK码点计为一个字/词 / Count each CJK code point as one word/char
                count++;
                inAsciiWord = false; // 重置ASCII单词状态 / Reset ASCII word state
            } else if (Character.isLetterOrDigit(cp)) {
                // group consecutive ASCII letters/digits as one word
                // 连续的ASCII字母/数字作为一个单词 / Group consecutive ASCII letters/digits as one word
                if (!inAsciiWord) {
                    count++; // 开始新的ASCII单词 / Start a new ASCII word
                    inAsciiWord = true; // 设置在ASCII单词中 / Set in ASCII word
                }
            } else {
                inAsciiWord = false; // 非字母数字字符，重置状态 / Non-alphanumeric character, reset state
            }
            i += charCount; // 移动到下一个字符 / Move to next character
        }
        return Math.max(count, 0); // 确保返回非负数 / Ensure non-negative result
    }


    /**
     * Check if a Unicode code point belongs to CJK (Chinese, Japanese, Korean) character blocks.
     * Includes various CJK unified ideographs, compatibility ideographs, and phonetic extensions.
     * 检查 Unicode 码点是否属于中日韩 (CJK) 字符块。
     * 包括各种 CJK 统一表意文字、兼容表意文字和音标扩展。
     *
     * @param codePoint the Unicode code point / Unicode 码点
     * @return true if CJK character / 如果是 CJK 字符则返回 true
     */
    private static boolean isCJK(int codePoint) {
        return CJK_BLOCKS.contains(Character.UnicodeBlock.of(codePoint));
    }

    /**
     * Unified word count API without slug support.
     * If name provided, count by name; otherwise sum word counts across all posts
     * (release/draft selectable by version).
     *
     * @param params parameter map: name? version? ('release'|'draft', default 'release')
     * @return word count as Mono (non-negative)
     */
    @Override
    public Mono<Integer> wordCount(Map<String, Object> params) {
        Map<String, Object> map = params == null ? java.util.Collections.emptyMap() : params;
        String name = String.valueOf(map.get("name"));
        String version = String.valueOf(map.getOrDefault("version", "release")).toLowerCase();

        if ("null".equals(name) || name.isBlank()) {
            name = null;
        }

        boolean isDraft = switch (version) {
            case "draft" -> true;
            case "release" -> false;
            default -> false; // 默认为发布版本 / Default to release version
        };

        if (name != null) {
            return isDraft ? contentCountByName(name, "getHeadContent")
                : contentCountByName(name, "getReleaseContent");
        }
        return countAllPosts(isDraft);
    }

    private Mono<Integer> countAllPosts(boolean isDraft) {
        final int pageSize = 100; // 分页大小 / Page size
        return sumPage(1, pageSize, isDraft, 0);
    }

    private Mono<Integer> sumPage(int page, int size, boolean isDraft, int acc) {
        var options = ListOptions.builder().build();
        return client.listBy(Post.class, options,
                PageRequestImpl.of(page, size, Sort.by("metadata.name")))
            .flatMap(result -> {
                var items = result.getItems(); // 获取文章列表 / Get post list
                if (items == null || items.isEmpty()) {
                    return Mono.just(acc); // 无数据时返回累积值 / Return accumulated value when no data
                }
                return Flux.fromIterable(items) // 转换为Flux流 / Convert to Flux stream
                    .map(p -> p.getMetadata().getName()) // 提取文章名称 / Extract post name
                    .flatMapSequential(
                        name -> isDraft ? contentCountByName(name, "getHeadContent")
                            : contentCountByName(name, "getReleaseContent"),
                        8) // 并发计算字数 / Calculate word count concurrently
                    .reduce(0, Integer::sum) // 累计字数 / Sum word counts
                    .flatMap(sumThis -> {
                        if (items.size() < size) {
                            return Mono.just(acc + sumThis); // 最后一页，返回总计 / Last page, return total
                        }
                        return sumPage(page + 1, size, isDraft,
                            acc + sumThis); // 递归处理下一页 / Recursively process next page
                    });
            })
            .switchIfEmpty(Mono.just(acc)); // 空结果时返回累积值 / Return accumulated value if empty
    }
}
