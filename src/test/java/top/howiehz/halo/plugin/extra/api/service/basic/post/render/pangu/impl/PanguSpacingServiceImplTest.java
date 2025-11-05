package top.howiehz.halo.plugin.extra.api.service.basic.post.render.pangu.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
}
