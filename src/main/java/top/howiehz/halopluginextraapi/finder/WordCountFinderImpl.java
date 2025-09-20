package top.howiehz.halopluginextraapi.finder;

import static run.halo.app.extension.index.query.QueryFactory.equal;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.content.Post;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.theme.finders.Finder;

/**
 * Implementation of WordCountFinder.
 */
@Component
@Finder("extraApi")
public class WordCountFinderImpl implements WordCountFinder {

    private final ReactiveExtensionClient client;
    private final ApplicationContext applicationContext;

    public WordCountFinderImpl(ReactiveExtensionClient client,
        ApplicationContext applicationContext) {
        this.client = client;
        this.applicationContext = applicationContext;
    }

    /**
     * Get the word count of the release content by post name.
     * 根据文章名称获取发布内容的字数统计。
     *
     * @param name the post name / 文章名称
     * @return word count as Mono / 返回字数统计的 Mono
     */
    @Override
    public Mono<Integer> releaseCountByName(String name) {
        return contentCountByName(name, "getReleaseContent");
    }

    /**
     * Get the word count of the head content by post name.
     * 根据文章名称获取头部内容的字数统计。
     *
     * @param name the post name / 文章名称
     * @return word count as Mono / 返回字数统计的 Mono
     */
    @Override
    public Mono<Integer> headCountByName(String name) {
        return contentCountByName(name, "getHeadContent");
    }

    /**
     * Get the word count of the release content by post slug.
     * 根据文章别名获取发布内容的字数统计。
     *
     * @param slug the post slug / 文章别名
     * @return word count as Mono / 返回字数统计的 Mono
     */
    @Override
    public Mono<Integer> releaseCountBySlug(String slug) {
        return resolveNameBySlug(slug).flatMap(this::releaseCountByName);
    }

    /**
     * Get the word count of the head content by post slug.
     * 根据文章别名获取头部内容的字数统计。
     *
     * @param slug the post slug / 文章别名
     * @return word count as Mono / 返回字数统计的 Mono
     */
    @Override
    public Mono<Integer> headCountBySlug(String slug) {
        return resolveNameBySlug(slug).flatMap(this::headCountByName);
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
            return Mono.just(0);
        }
        final var svc = getPostContentService();
        if (svc == null) {
            // Service unavailable at compile/runtime mismatch
            return Mono.just(0);
        }
        try {
            Method m = svc.getClass().getMethod(methodName, String.class);
            @SuppressWarnings("unchecked")
            Mono<?> mono = (Mono<?>) m.invoke(svc, name);
            return mono
                .map(pc -> safeCount(extractText(getContent(pc))))
                .onErrorReturn(0)
                .defaultIfEmpty(0);
        } catch (Throwable e) {
            return Mono.just(0);
        }
    }

    /**
     * Resolve post name by slug.
     * 根据别名解析文章名称。
     *
     * @param slug the post slug / 文章别名
     * @return post name as Mono / 返回文章名称的 Mono
     */
    private Mono<String> resolveNameBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return Mono.just("");
        }
        var options = ListOptions.builder()
            .fieldQuery(equal("spec.slug", slug))
            .build();
        return client.listBy(Post.class, options,
                PageRequestImpl.of(1, 1, Sort.by("metadata.name")))
            .map(list -> list.getItems().stream().findFirst().map(p -> p.getMetadata().getName())
                .orElse(""))
            .defaultIfEmpty("");
    }

    private final AtomicReference<Object> postContentServiceRef = new AtomicReference<>();

    /**
     * Get PostContentService instance from application context.
     * Uses caching and tries multiple known class names for compatibility.
     * 从应用程序上下文获取 PostContentService 实例。
     * 使用缓存并尝试多个已知类名以确保兼容性。
     *
     * @return PostContentService instance or null if not found / 返回 PostContentService
     * 实例，如果未找到则返回 null
     */
    private Object getPostContentService() {
        var cached = postContentServiceRef.get();
        if (cached != null) {
            return cached;
        }
        // Try by known FQCNs
        String[] candidates = new String[] {
            "run.halo.app.core.post.PostContentService",
            "run.halo.app.core.extension.content.PostContentService",
            "run.halo.app.core.content.PostContentService"
        };
        for (String fqcn : candidates) {
            try {
                Class<?> clazz = Class.forName(fqcn);
                Object bean = applicationContext.getBean(clazz);
                if (bean != null) {
                    postContentServiceRef.compareAndSet(null, bean);
                    return bean;
                }
            } catch (Throwable ignored) {
            }
        }
        // Fallback by common bean name
        try {
            Object bean = applicationContext.getBean("postContentService");
            if (bean != null) {
                postContentServiceRef.compareAndSet(null, bean);
                return bean;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * Extract content string from PostContent object.
     * Tries getContent() method first, then content() method as fallback.
     * 从 PostContent 对象提取内容字符串。
     * 首先尝试 getContent() 方法，然后使用 content() 方法作为备用。
     *
     * @param postContent the post content object / 文章内容对象
     * @return content string / 返回内容字符串
     */
    private static String getContent(Object postContent) {
        if (postContent == null) {
            return "";
        }
        try {
            // Prefer getContent()
            var method = postContent.getClass().getMethod("getContent");
            Object content = method.invoke(postContent);
            return Objects.toString(content, "");
        } catch (Exception ignored) {
        }
        try {
            // Fallback to content() accessor (record style)
            var method = postContent.getClass().getMethod("content");
            Object content = method.invoke(postContent);
            return Objects.toString(content, "");
        } catch (Exception ignored) {
        }
        return postContent.toString();
    }

    // Patterns for stripping HTML quickly
    private static final Pattern SCRIPT = Pattern.compile("(?is)<script[^>]*>.*?</script>");
    private static final Pattern STYLE = Pattern.compile("(?is)<style[^>]*>.*?</style>");
    private static final Pattern TAG = Pattern.compile("(?s)<[^>]+>");
    private static final Pattern ENTITY_NBSP = Pattern.compile("&nbsp;", Pattern.CASE_INSENSITIVE);

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
        String text = SCRIPT.matcher(html).replaceAll(" ");
        text = STYLE.matcher(text).replaceAll(" ");
        text = TAG.matcher(text).replaceAll(" ");
        text = ENTITY_NBSP.matcher(text).replaceAll(" ");
        // Collapse whitespace
        text = text.replace('\u00A0', ' ');
        text = text.replaceAll("[\\r\\n\\t]", " ");
        text = text.replaceAll("\\s+", " ");
        // Remove space before common punctuation (ASCII and CJK)
        text = text.replaceAll("\\s+([,\\.!\\?:;])", "$1");
        text = text.replaceAll("\\s+([，。！？：；、])", "$1");
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
    static int safeCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int count = 0;
        boolean inAsciiWord = false;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            int charCount = Character.charCount(cp);
            if (isCJK(cp)) {
                // count each CJK code point as one word/char
                count++;
                inAsciiWord = false;
            } else if (Character.isLetterOrDigit(cp)) {
                // group consecutive ASCII letters/digits as one word
                if (!inAsciiWord) {
                    count++;
                    inAsciiWord = true;
                }
            } else {
                inAsciiWord = false;
            }
            i += charCount;
        }
        return Math.max(count, 0);
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
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_E
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_F
            || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT
            || block == Character.UnicodeBlock.HIRAGANA
            || block == Character.UnicodeBlock.KATAKANA
            || block == Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS
            || block == Character.UnicodeBlock.HANGUL_SYLLABLES
            || block == Character.UnicodeBlock.HANGUL_JAMO
            || block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO;
    }
}
