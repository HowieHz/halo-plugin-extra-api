package top.howiehz.halo.plugin.extra.api.finder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ExtraApiStatsFinderImplTest {
    @Test
    void testExtractTextNullInput() {
        // 测试 null 输入
        String result = ExtraApiStatsFinderImpl.extractText(null);
        assertEquals("", result);
    }

    @Test
    void testExtractTextBlankInput() {
        // 测试空白输入
        String result = ExtraApiStatsFinderImpl.extractText("   ");
        assertEquals("", result);
    }

    @Test
    void testExtractTextBasicHtml() {
        // 测试基本 HTML 标签移除
        String html = "<p>Hello <strong>world</strong>!</p>";
        String result = ExtraApiStatsFinderImpl.extractText(html);
        assertEquals(" Hello  world ! ", result);
    }

    @Test
    void testExtractTextScriptAndStyle() {
        // 测试 script 和 style 标签移除
        String html = "<html><head><style>body{color:red;}</style></head>" +
            "<body><script>alert('test');</script><p>Content</p></body></html>";
        String result = ExtraApiStatsFinderImpl.extractText(html);
        assertEquals("       Content   ", result);
    }

    @Test
    void extractTextShouldStripHtmlAndCollapseWhitespace() {
        String html = "<p>Hello <b>world</b>!<script>alert(1)</script>\n<style>p{}</style></p>";
        String text = ExtraApiStatsFinderImpl.extractText(html);
        assertEquals(" Hello  world ! \n"
            + "  ", text);
    }

    @Test
    void testExtractTextEntities() {
        // 测试 HTML 实体处理
        String html = "<p>Hello&nbsp;world&nbsp;test</p>";
        String result = ExtraApiStatsFinderImpl.extractText(html);
        assertEquals(" Hello world test ", result);
    }

    @Test
    void testExtractTextWhitespaceNormalization() {
        // 测试空白字符规范化
        String html = "<p>Hello\n\t  world\r\n!</p>";
        String result = ExtraApiStatsFinderImpl.extractText(html);
        assertEquals(" Hello\n\t  world\r\n! ", result);
    }

    @Test
    void testExtractTextPunctuationSpacing() {
        // 测试标点符号前空格处理
        String html = "<p>Hello , world .</p><p>你好 ，世界 。</p>";
        String result = ExtraApiStatsFinderImpl.extractText(html);
        assertEquals(" Hello , world .  你好 ，世界 。 ", result);
    }

    @Test
    void testCountWordsNullAndEmpty() {
        // 测试 null 和空字符串
        assertEquals(0, ExtraApiStatsFinderImpl.countWords(null));
        assertEquals(0, ExtraApiStatsFinderImpl.countWords(""));
    }

    @Test
    void safeCountAsciiWords() {
        // 测试英文单词计数
        assertEquals(3, ExtraApiStatsFinderImpl.countWords("Hello world test"));
        assertEquals(4, ExtraApiStatsFinderImpl.countWords("Hello123 world-test abc"));
        assertEquals(2, ExtraApiStatsFinderImpl.countWords("Hello   world"));
        assertEquals(2, ExtraApiStatsFinderImpl.countWords("Hello world!"));
        assertEquals(3, ExtraApiStatsFinderImpl.countWords("Hello, Halo 2"));
        assertEquals(2, ExtraApiStatsFinderImpl.countWords("10.11"));
    }

    @Test
    void safeCountCjkCharacters() {
        // 测试中日韩字符计数
        assertEquals(4, ExtraApiStatsFinderImpl.countWords("你好，世界！"));
        assertEquals(5, ExtraApiStatsFinderImpl.countWords("こんにちは"));
        assertEquals(6, ExtraApiStatsFinderImpl.countWords("안녕하십니까"));
    }

    @Test
    void testCountWordsMixedContent() {
        // 测试混合内容
        assertEquals(4, ExtraApiStatsFinderImpl.countWords("Hello 你好 world"));
        assertEquals(6, ExtraApiStatsFinderImpl.countWords("test123 测试 hello 世界"));
        assertEquals(4, ExtraApiStatsFinderImpl.countWords("Hello,你好！world"));
        assertEquals(1, ExtraApiStatsFinderImpl.countWords("Hello123123test"));
    }

    @Test
    void testCountWordsUnicodeNormalization() {
        // 测试 Unicode 标准化
        String combined = "A\u030A"; // A + 环形符 = Å
        String single = "\u00C5";    // 直接的 Å

        // 标准化后应该得到相同的字数
        assertEquals(1, ExtraApiStatsFinderImpl.countWords(combined));
        assertEquals(1, ExtraApiStatsFinderImpl.countWords(single));
    }

    @Test
    void testCountWordsSpecialCharacters() {
        // 测试特殊字符和标点符号
        assertEquals(0, ExtraApiStatsFinderImpl.countWords("\u00A0\t\n\n"));
        assertEquals(0, ExtraApiStatsFinderImpl.countWords("!!@#$%^&*()"));
    }
}
