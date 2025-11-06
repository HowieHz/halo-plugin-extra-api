package top.howiehz.halo.plugin.extra.api.service.basic.post.render.pangu.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for PanguSpacingServiceImpl.
 * PanguSpacingServiceImpl 的单元测试。
 *
 * @author HowieXie
 * @since 1.0.0
 */
class PanguSpacingServiceImplTest {

    private PanguSpacingServiceImpl panguSpacingService;

    @BeforeEach
    void setUp() {
        panguSpacingService = new PanguSpacingServiceImpl();
    }

    // ========== Tests for applySpacingInText ==========

    @Test
    void testApplySpacingInText_withCJKAndEnglish() {
        String input = "請問Jackie的鼻子有幾個？123個！";
        String expected = "請問 Jackie 的鼻子有幾個？123 個！";

        String result = panguSpacingService.applySpacingInText(input);

        assertEquals(expected, result);
    }

    @Test
    void testApplySpacingInText_withEmptyString() {
        String input = "";
        String result = panguSpacingService.applySpacingInText(input);
        assertEquals("", result);
    }

    @Test
    void testApplySpacingInText_withNull() {
        String result = panguSpacingService.applySpacingInText(null);
        assertEquals("", result);
    }

    // ========== Tests for applySpacingInHtml(String) ==========

    @Test
    void testApplySpacingInHtml_withSimpleContent() {
        String input = "<p>這是一個test測試123數字</p>";

        String result = panguSpacingService.applySpacingInHtml(input);

        assertTrue(result.contains("test 測試"));
        assertTrue(result.contains("測試 123 數字"));
    }

    @Test
    void testApplySpacingInHtml_preservesCodeBlocks() {
        String input = "<p>這是test</p><code>這是code不應該處理</code>";

        String result = panguSpacingService.applySpacingInHtml(input);

        // Paragraph should be processed
        assertTrue(result.contains("這是 test"));
        // Code tag content should remain unchanged
        assertTrue(result.contains("這是code不應該處理"));
    }

    @Test
    void testApplySpacingInHtml_withMultipleParagraphs() {
        String input = "<p>第一段test1</p><p>第二段test2</p>";

        String result = panguSpacingService.applySpacingInHtml(input);

        assertTrue(result.contains("第一段 test1"));
        assertTrue(result.contains("第二段 test2"));
    }

    @Test
    void testApplySpacingInHtml_withNestedElements() {
        String input = "<div><p>外層test<strong>內層strong123數字</strong></p></div>";

        String result = panguSpacingService.applySpacingInHtml(input);

        assertTrue(result.contains("外層 test"));
        assertTrue(result.contains("strong123 數字"));
    }

    @Test
    void testApplySpacingInHtml_withEmptyString() {
        String input = "";
        String result = panguSpacingService.applySpacingInHtml(input);
        assertEquals("", result);
    }

    @Test
    void testApplySpacingInHtml_withNull() {
        String result = panguSpacingService.applySpacingInHtml((String) null);
        assertEquals("", result);
    }

    // ========== Tests for applySpacingInHtml(Map) ==========

    @Test
    void testApplySpacingInHtml_withMapNoSelector() {
        String html = "<p>這是test內容</p>";
        Map<String, Object> params = Map.of("htmlContent", html);

        String result = panguSpacingService.applySpacingInHtml(params);

        assertTrue(result.contains("這是 test 內容"));
    }

    @Test
    void testApplySpacingInHtml_withTagSelector() {
        String html = "<p>段落test</p><div>div內容test2</div>";
        Map<String, Object> params = Map.of(
            "htmlContent", html,
            "selector", "p"
        );

        String result = panguSpacingService.applySpacingInHtml(params);

        // Only p tag should be processed
        assertTrue(result.contains("段落 test"));
        // div should not be processed
        assertTrue(result.contains("div內容test2"));
    }

    @Test
    void testApplySpacingInHtml_withClassSelector() {
        String html = "<div class=\"content\">這是test</div><div class=\"other\">其他test2</div>";
        Map<String, Object> params = Map.of(
            "htmlContent", html,
            "selector", ".content"
        );

        String result = panguSpacingService.applySpacingInHtml(params);

        // Only .content should be processed
        assertTrue(result.contains("這是 test"));
        // .other should not be processed
        assertTrue(result.contains("其他test2"));
    }

    @Test
    void testApplySpacingInHtml_withIdSelector() {
        String html = "<div id=\"main\">主要test</div><div id=\"side\">側邊test2</div>";
        Map<String, Object> params = Map.of(
            "htmlContent", html,
            "selector", "#main"
        );

        String result = panguSpacingService.applySpacingInHtml(params);

        // Only #main should be processed
        assertTrue(result.contains("主要 test"));
        // #side should not be processed
        assertTrue(result.contains("側邊test2"));
    }

    @Test
    void testApplySpacingInHtml_withComplexSelector() {
        String html = "<div class=\"article\"><p>文章test</p></div><div><p>其他test2</p></div>";
        Map<String, Object> params = Map.of(
            "htmlContent", html,
            "selector", "div.article p"
        );

        String result = panguSpacingService.applySpacingInHtml(params);

        // Only p inside div.article should be processed
        assertTrue(result.contains("文章 test"));
        // Other p should not be processed
        assertTrue(result.contains("其他test2"));
    }

    @Test
    void testApplySpacingInHtml_withNonExistentSelector() {
        String html = "<p>測試test</p>";
        Map<String, Object> params = Map.of(
            "htmlContent", html,
            "selector", "span"
        );

        String result = panguSpacingService.applySpacingInHtml(params);

        // Should return original content when no elements match
        assertEquals(html, result);
    }

    @Test
    void testApplySpacingInHtml_withEmptyParams() {
        Map<String, Object> params = Map.of();

        String result = panguSpacingService.applySpacingInHtml(params);

        assertEquals("", result);
    }

    @Test
    void testApplySpacingInHtml_withNullParams() {
        String result = panguSpacingService.applySpacingInHtml((Map<String, Object>) null);

        assertEquals("", result);
    }

    @Test
    void testApplySpacingInHtml_withMissingHtmlContent() {
        Map<String, Object> params = Map.of("selector", "p");

        String result = panguSpacingService.applySpacingInHtml(params);

        assertEquals("", result);
    }

    @Test
    void testApplySpacingInHtml_preservesCodeBlocksWithSelector() {
        String html = "<div class=\"content\">這是test<code>代碼code不處理</code></div>";
        Map<String, Object> params = Map.of(
            "htmlContent", html,
            "selector", ".content"
        );

        String result = panguSpacingService.applySpacingInHtml(params);

        // Text should be processed
        assertTrue(result.contains("這是 test"));
        // Code should not be processed
        assertTrue(result.contains("代碼code不處理"));
    }

    @Test
    void testApplySpacingInHtml_withNestedElementsAndSelector() {
        String html =
            "<div class=\"wrapper\"><p>外層test<strong>內層strong123數字</strong></p></div>";
        Map<String, Object> params = Map.of(
            "htmlContent", html,
            "selector", ".wrapper"
        );

        String result = panguSpacingService.applySpacingInHtml(params);

        assertTrue(result.contains("外層 test"));
        assertTrue(result.contains("strong123 數字"));
    }
}
