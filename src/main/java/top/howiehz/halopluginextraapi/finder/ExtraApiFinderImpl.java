package top.howiehz.halopluginextraapi.finder;

import static run.halo.app.extension.index.query.QueryFactory.equal;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.content.PostContentService;
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
public class ExtraApiFinderImpl implements ExtraApiFinder {

    private final ReactiveExtensionClient client; // 响应式扩展客户端 / Reactive extension client
    private final PostContentService postContentService; // 文章内容服务 / Post content service
    private final ApplicationContext applicationContext; // Spring应用上下文 / Spring application context

    /**
     * Constructor to initialize ExtraApiFinderImpl with required dependencies.
     * 构造函数，使用必需的依赖项初始化 ExtraApiFinderImpl。
     */
    public ExtraApiFinderImpl(ReactiveExtensionClient client, PostContentService postContentService,
        ApplicationContext applicationContext) {
        this.client = client; // 注入响应式扩展客户端 / Inject reactive extension client
        this.postContentService = postContentService; // 注入文章内容服务 / Inject post content service
        this.applicationContext = applicationContext; // 注入应用上下文 / Inject application context
    }

    /**
     * Get the word count of the release content by post name.
     * 根据文章名称获取发布内容的字数统计。
     *
     * @param name the post name / 文章名称
     * @return word count as Mono / 返回字数统计的 Mono
     */
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
            return Mono.just(0); // 空名称直接返回0 / Return 0 for empty name
        }
        final var svc = getPostContentService(); // 获取文章内容服务 / Get post content service
        if (svc == null) {
            // Service unavailable at compile/runtime mismatch
            // 服务不可用，编译/运行时不匹配 / Service unavailable at compile/runtime mismatch
            return Mono.just(0);
        }
        try {
            Method m = svc.getClass()
                .getMethod(methodName, String.class); // 反射获取方法 / Get method by reflection
            @SuppressWarnings("unchecked")
            Mono<?> mono = (Mono<?>) m.invoke(svc, name); // 调用方法获取内容 / Invoke method to get content
            return mono
                .map(pc -> safeCount(extractText(getContent(pc)))) // 提取并计数 / Extract and count
                .onErrorReturn(0) // 出错时返回0 / Return 0 on error
                .defaultIfEmpty(0); // 空结果时返回0 / Return 0 if empty
        } catch (Throwable e) {
            return Mono.just(0); // 异常时返回0 / Return 0 on exception
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
            return Mono.just(""); // 空别名返回空字符串 / Return empty string for null/blank slug
        }
        var options = ListOptions.builder()
            .fieldQuery(equal("spec.slug", slug)) // 按别名查询 / Query by slug
            .build();
        return client.listBy(Post.class, options,
                PageRequestImpl.of(1, 1,
                    Sort.by("metadata.name"))) // 分页查询，只取第一个 / Paginated query, take first only
            .map(list -> list.getItems().stream().findFirst().map(p -> p.getMetadata().getName())
                .orElse("")) // 提取文章名称或返回空 / Extract post name or return empty
            .defaultIfEmpty(""); // 查询结果为空时返回空字符串 / Return empty string if no result
    }

    private final AtomicReference<Object> postContentServiceRef = new AtomicReference<>();
        // 缓存的文章内容服务引用 / Cached post content service reference
    private final AtomicReference<Object> postFinderRef = new AtomicReference<>();
        // 缓存的文章查找器引用 / Cached post finder reference

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
        // Prefer injected service if available
        // 优先使用已注入的服务（如果可用） / Prefer injected service if available
        if (this.postContentService != null) {
            return this.postContentService;
        }
        var cached = postContentServiceRef.get(); // 获取缓存的服务实例 / Get cached service instance
        if (cached != null) {
            return cached;
        }
        // Try by known FQCNs
        // 尝试通过已知的完全限定类名查找 / Try by known FQCNs
        String[] candidates = new String[] {
            "run.halo.app.core.post.PostContentService",
            "run.halo.app.core.extension.content.PostContentService",
            "run.halo.app.core.content.PostContentService"
        };
        for (String fqcn : candidates) {
            try {
                Class<?> clazz = Class.forName(fqcn); // 加载类 / Load class
                Object bean = applicationContext.getBean(clazz); // 获取Bean实例 / Get bean instance
                if (bean != null) {
                    postContentServiceRef.compareAndSet(null, bean); // 缓存实例 / Cache instance
                    return bean;
                }
            } catch (Throwable ignored) {
                // 忽略异常，继续尝试下一个候选类 / Ignore exception, try next candidate
            }
        }
        // Fallback by common bean name
        // 通过通用Bean名称回退查找 / Fallback by common bean name
        try {
            Object bean = applicationContext.getBean("postContentService");
            if (bean != null) {
                postContentServiceRef.compareAndSet(null, bean); // 缓存实例 / Cache instance
                return bean;
            }
        } catch (Throwable ignored) {
            // 忽略异常 / Ignore exception
        }
        return null; // 未找到服务 / Service not found
    }

    /**
     * Resolve built-in PostFinder bean for delegation.
     * 解析内置的PostFinder bean用于委派。
     */
    private Object getPostFinder() {
        var cached = postFinderRef.get(); // 获取缓存的查找器实例 / Get cached finder instance
        if (cached != null) {
            return cached;
        }
        String[] candidates = new String[] {
            "run.halo.app.theme.finders.PostFinder"
            // 候选PostFinder类名 / Candidate PostFinder class name
        };
        for (String fqcn : candidates) {
            try {
                Class<?> clazz = Class.forName(fqcn); // 加载类 / Load class
                Object bean = applicationContext.getBean(clazz); // 获取Bean实例 / Get bean instance
                if (bean != null) {
                    postFinderRef.compareAndSet(null, bean); // 缓存实例 / Cache instance
                    return bean;
                }
            } catch (Throwable ignored) {
                // 忽略异常，继续尝试下一个候选类 / Ignore exception, try next candidate
            }
        }
        try {
            Object bean =
                applicationContext.getBean("postFinder"); // 通过Bean名称查找 / Find by bean name
            if (bean != null) {
                postFinderRef.compareAndSet(null, bean); // 缓存实例 / Cache instance
                return bean;
            }
        } catch (Throwable ignored) {
            // 忽略异常 / Ignore exception
        }
        return null; // 未找到查找器 / Finder not found
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
            // 优先使用 getContent() 方法 / Prefer getContent() method
            var method = postContent.getClass().getMethod("getContent");
            Object content = method.invoke(postContent);
            return Objects.toString(content, "");
        } catch (Exception ignored) {
            // 忽略异常，尝试备用方法 / Ignore exception, try fallback method
        }
        try {
            // Fallback to content() accessor (record style)
            // 回退到 content() 访问器（记录样式） / Fallback to content() accessor (record style)
            var method = postContent.getClass().getMethod("content");
            Object content = method.invoke(postContent);
            return Objects.toString(content, "");
        } catch (Exception ignored) {
            // 忽略异常，使用默认字符串 / Ignore exception, use default string
        }
        return postContent.toString(); // 最后回退到 toString() / Final fallback to toString()
    }

    // Patterns for stripping HTML quickly
    // 快速移除HTML的正则表达式模式 / Patterns for quickly stripping HTML
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
    static int safeCount(String text) {
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

    /**
     * Unified post list query.
     * Accepts a parameter map with optional keys: page, size, tagName, categoryName, ownerName,
     * sort.
     * Delegates to Halo built-in postFinder.list(Map) when available for best compatibility.
     * <p>
     * Params example (from template):
     * postFinder.list({ page: 1, size: 10, tagName: 'foo', categoryName: 'bar', ownerName:
     * 'admin', sort: {'spec.publishTime,desc','metadata.creationTimestamp,asc'} })
     *
     * @param params map of query parameters
     * @return Mono of page result compatible with Halo templates
     */
    @Override
    public Mono<?> list(Map<String, Object> params) {
        Map<String, Object> safe = params == null ? Collections.emptyMap() : params;
        // Try delegate to built-in postFinder for full feature support (tags/categories/owner
        // filters, etc.)
        // 尝试委派给内置的postFinder以获得完整功能支持（标签/分类/所有者过滤等） / Try delegate to built-in postFinder for
        // full feature support
        Object postFinder = getPostFinder();
        if (postFinder != null) {
            try {
                Method listMethod = postFinder.getClass().getMethod("list", Map.class);
                Object result = listMethod.invoke(postFinder, safe);
                if (result instanceof Mono) {
                    return (Mono<?>) result;
                }
                return Mono.justOrEmpty(result);
            } catch (Throwable ignored) {
                // fall through to fallback implementation
                // 失败时转到备用实现 / Fall through to fallback implementation
            }
        }
        // Fallback: only page/size/sort supported here; filters may require core finder
        // 备用方案：这里只支持分页/大小/排序；过滤器可能需要核心查找器 / Fallback: only basic pagination supported
        int page = getInt(safe.get("page"), 1);
        int size = getInt(safe.get("size"), 10);
        Sort sort = toSort(safe.get("sort"));
        var options = ListOptions.builder().build();
        return client.listBy(Post.class, options, PageRequestImpl.of(page, size, sort));
    }

    private static int getInt(Object v, int def) {
        if (v == null) {
            return def; // 空值返回默认值 / Return default value for null
        }
        if (v instanceof Number n) {
            return n.intValue(); // 数字类型直接转换 / Convert Number type directly
        }
        try {
            return Integer.parseInt(
                String.valueOf(v)); // 尝试解析字符串为整数 / Try to parse string as integer
        } catch (Exception e) {
            return def; // 解析失败返回默认值 / Return default value on parse failure
        }
    }

    private static Sort toSort(Object sortParam) {
        if (sortParam == null) {
            return Sort.unsorted();
        }
        List<String> parts = new ArrayList<>();
        if (sortParam instanceof String s) {
            parts.add(s); // 字符串类型直接添加 / Add string type directly
        } else if (sortParam instanceof String[] arr) {
            parts.addAll(Arrays.asList(arr)); // 字符串数组转换为列表 / Convert string array to list
        } else if (sortParam instanceof Collection<?> c) {
            for (Object o : c) {
                if (o != null) {
                    parts.add(String.valueOf(
                        o)); // 集合中的每个元素转为字符串 / Convert each element in collection to string
                }
            }
        } else {
            parts.add(String.valueOf(sortParam)); // 其他类型转为字符串 / Convert other types to string
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (String item : parts) {
            if (item == null || item.isBlank()) {
                continue; // 跳过空值 / Skip null/blank values
            }
            String[] seg = item.split(",", 2); // 分割属性和方向 / Split property and direction
            String prop = seg[0].trim(); // 属性名 / Property name
            String dir = seg.length > 1 ? seg[1].trim()
                : "asc"; // 排序方向，默认升序 / Sort direction, default ascending
            orders.add("desc".equalsIgnoreCase(dir) ? Sort.Order.desc(prop) : Sort.Order.asc(prop));
        }
        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders); // 返回排序对象 / Return sort object
    }

    /**
     * Unified word count API.
     * If name provided, count by name; else if slug provided, count by slug.
     * If neither provided, sum word counts across all posts (release/head selectable by version).
     *
     * @param params parameter map: name? slug? version? ('release'|'head', default 'release')
     * @return word count as Mono (non-negative)
     */
    @Override
    public Mono<Integer> wordCount(Map<String, Object> params) {
        Map<String, Object> map = params == null ? java.util.Collections.emptyMap() : params;
        String name = toString(map.get("name"));
        String slug = toString(map.get("slug"));
        String version = toString(map.getOrDefault("version", "release")).toLowerCase();
        boolean head = "head".equals(version);
        if (name != null && !name.isBlank()) {
            return head ? headCountByName(name) : releaseCountByName(name);
        }
        if (slug != null && !slug.isBlank()) {
            return head ? headCountBySlug(slug) : releaseCountBySlug(slug);
        }
        // Neither name nor slug provided: count all posts
        // 未提供名称或别名：统计所有文章 / Neither name nor slug provided: count all posts
        return countAllPosts(head);
    }

    private Mono<Integer> countAllPosts(boolean head) {
        final int pageSize = 100; // 分页大小 / Page size
        return sumPage(1, pageSize, head, 0);
    }

    private Mono<Integer> sumPage(int page, int size, boolean head, int acc) {
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
                        name -> head ? headCountByName(name) : releaseCountByName(name),
                        8) // 并发计算字数 / Calculate word count concurrently
                    .reduce(0, Integer::sum) // 累计字数 / Sum word counts
                    .flatMap(sumThis -> {
                        if (items.size() < size) {
                            return Mono.just(acc + sumThis); // 最后一页，返回总计 / Last page, return total
                        }
                        return sumPage(page + 1, size, head,
                            acc + sumThis); // 递归处理下一页 / Recursively process next page
                    });
            })
            .switchIfEmpty(Mono.just(acc)); // 空结果时返回累积值 / Return accumulated value if empty
    }

    private static String toString(Object o) {
        return o == null ? null : String.valueOf(
            o); // 将对象转换为字符串，空对象返回null / Convert object to string, return null for null objects
    }
}
