package top.howiehz.halo.plugin.extra.api.service.basic.post.render.pangu.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void testSpacingText_withCJKAndEnglish() {
        String input = "請問Jackie的鼻子有幾個？123個！";
        String expected = "請問 Jackie 的鼻子有幾個？123 個！";

        String result = panguSpacingService.spacingText(input);

        assertEquals(expected, result);
    }

    @Test
    void testSpacingText_withEmptyString() {
        String input = "";
        String result = panguSpacingService.spacingText(input);
        assertEquals("", result);
    }

    @Test
    void testSpacingText_withNull() {
        String result = panguSpacingService.spacingText(null);
        assertNull(result);
    }

    @Test
    void testSpacingElementByTagName_withParagraph() {
        String input = "<p>這是一個test測試123數字</p>";
        String tagName = "p";

        String result = panguSpacingService.spacingElementByTagName(input, tagName);

        assertTrue(result.contains("test 測試"));
        assertTrue(result.contains("測試 123 數字"));
    }

    @Test
    void testSpacingElementByTagName_preservesCodeBlocks() {
        String input = "<p>這是test</p><code>這是code不應該處理</code>";
        String tagName = "p";

        String result = panguSpacingService.spacingElementByTagName(input, tagName);

        // 段落应该被处理
        assertTrue(result.contains("這是 test"));
        // code 标签中的内容应该保持不变
        assertTrue(result.contains("這是code不應該處理"));
    }

    @Test
    void testSpacingElementByTagName_withMultipleParagraphs() {
        String input = "<p>第一段test1</p><p>第二段test2</p>";
        String tagName = "p";

        String result = panguSpacingService.spacingElementByTagName(input, tagName);

        assertTrue(result.contains("第一段 test1"));
        assertTrue(result.contains("第二段 test2"));
    }

    @Test
    void testSpacingElementByTagName_withNestedElements() {
        String input = "<div><p>外層test<strong>內層strong123數字</strong></p></div>";
        String tagName = "p";

        String result = panguSpacingService.spacingElementByTagName(input, tagName);

        assertTrue(result.contains("外層 test"));
        assertTrue(result.contains("strong123 數字"));
    }

    @Test
    void testSpacingElementByTagName_withEmptyTagName() {
        String input = "<p>測試test</p>";
        String result = panguSpacingService.spacingElementByTagName(input, "");

        assertEquals(input, result);
    }

    @Test
    void testSpacingElementByTagName_withNullTagName() {
        String input = "<p>測試test</p>";
        String result = panguSpacingService.spacingElementByTagName(input, null);

        assertEquals(input, result);
    }

    @Test
    void testSpacingElementByTagName_withNonExistentTag() {
        String input = "<p>測試test</p>";
        String result = panguSpacingService.spacingElementByTagName(input, "span");

        // 应该返回原始内容，因为没有找到匹配的标签
        assertEquals(input, result);
    }

    // ========== Tests for spacingElementById ==========

    @Test
    void testSpacingElementById_withSingleElement() {
        String input = "<div id=\"content\">這是一個test測試123數字</div>";
        String id = "content";

        String result = panguSpacingService.spacingElementById(input, id);

        assertTrue(result.contains("這是一個 test 測試 123 數字"));
    }

    @Test
    void testSpacingElementById_preservesCodeBlocks() {
        String input = "<div id=\"main\">這是test<code>這是code不應該處理</code></div>";
        String id = "main";

        String result = panguSpacingService.spacingElementById(input, id);

        // div 中的文本应该被处理
        assertTrue(result.contains("這是 test"));
        // code 标签中的内容应该保持不变
        assertTrue(result.contains("這是code不應該處理"));
    }

    @Test
    void testSpacingElementById_withNestedElements() {
        String input =
            "<div id=\"wrapper\"><p>外層test<strong>內層strong123數字</strong></p></div>";
        String id = "wrapper";

        String result = panguSpacingService.spacingElementById(input, id);

        assertTrue(result.contains("外層 test"));
        assertTrue(result.contains("strong123 數字"));
    }

    @Test
    void testSpacingElementById_withEmptyId() {
        String input = "<div id=\"test\">測試test</div>";
        String result = panguSpacingService.spacingElementById(input, "");

        assertEquals(input, result);
    }

    @Test
    void testSpacingElementById_withNullId() {
        String input = "<div id=\"test\">測試test</div>";
        String result = panguSpacingService.spacingElementById(input, null);

        assertEquals(input, result);
    }

    @Test
    void testSpacingElementById_withNonExistentId() {
        String input = "<div id=\"test\">測試test</div>";
        String result = panguSpacingService.spacingElementById(input, "nonexistent");

        // 应该返回原始内容，因为没有找到匹配的 ID
        assertEquals(input, result);
    }

    @Test
    void testSpacingElementById_withMultipleElementsOnlyProcessOne() {
        String input = "<div id=\"first\">第一個test1</div><div id=\"second\">第二個test2</div>";
        String id = "first";

        String result = panguSpacingService.spacingElementById(input, id);

        // 只有第一个 div 应该被处理
        assertTrue(result.contains("第一個 test1"));
        // 第二个 div 不应该被处理
        assertTrue(result.contains("第二個test2"));
    }

    // ========== Tests for spacingElementByClassName ==========

    @Test
    void testSpacingElementByClassName_withSingleElement() {
        String input = "<div class=\"content\">這是一個test測試123數字</div>";
        String className = "content";

        String result = panguSpacingService.spacingElementByClassName(input, className);

        assertTrue(result.contains("這是一個 test 測試 123 數字"));
    }

    @Test
    void testSpacingElementByClassName_withMultipleElements() {
        String input = "<div class=\"text\">第一段test1</div><p class=\"text\">第二段test2</p>";
        String className = "text";

        String result = panguSpacingService.spacingElementByClassName(input, className);

        // 两个元素都应该被处理
        assertTrue(result.contains("第一段 test1"));
        assertTrue(result.contains("第二段 test2"));
    }

    @Test
    void testSpacingElementByClassName_preservesCodeBlocks() {
        String input = "<div class=\"article\">這是test<code>這是code不應該處理</code></div>";
        String className = "article";

        String result = panguSpacingService.spacingElementByClassName(input, className);

        // div 中的文本应该被处理
        assertTrue(result.contains("這是 test"));
        // code 标签中的内容应该保持不变
        assertTrue(result.contains("這是code不應該處理"));
    }

    @Test
    void testSpacingElementByClassName_withNestedElements() {
        String input =
            "<div class=\"container\"><p>外層test<strong>內層strong123數字</strong></p></div>";
        String className = "container";

        String result = panguSpacingService.spacingElementByClassName(input, className);

        assertTrue(result.contains("外層 test"));
        assertTrue(result.contains("strong123 數字"));
    }

    @Test
    void testSpacingElementByClassName_withEmptyClassName() {
        String input = "<div class=\"test\">測試test</div>";
        String result = panguSpacingService.spacingElementByClassName(input, "");

        assertEquals(input, result);
    }

    @Test
    void testSpacingElementByClassName_withNullClassName() {
        String input = "<div class=\"test\">測試test</div>";
        String result = panguSpacingService.spacingElementByClassName(input, null);

        assertEquals(input, result);
    }

    @Test
    void testSpacingElementByClassName_withNonExistentClass() {
        String input = "<div class=\"test\">測試test</div>";
        String result = panguSpacingService.spacingElementByClassName(input, "nonexistent");

        // 应该返回原始内容，因为没有找到匹配的 class
        assertEquals(input, result);
    }

    @Test
    void testSpacingElementByClassName_withMultipleClasses() {
        String input = "<div class=\"text highlight\">這是test內容</div>";
        String className = "text";

        String result = panguSpacingService.spacingElementByClassName(input, className);

        // 元素有多个 class，但只要包含指定的 class 就应该被处理
        assertTrue(result.contains("這是 test 內容"));
    }
}
