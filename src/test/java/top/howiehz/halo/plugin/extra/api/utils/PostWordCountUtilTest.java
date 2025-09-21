package top.howiehz.halo.plugin.extra.api.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;

public class PostWordCountUtilTest {
    @Test
    void testExtractTextNullInput() {
        // 测试 null 输入
        String result = PostWordCountUtil.extractText(null);
        assertEquals("", result);
    }

    @Test
    void testExtractTextBlankInput() {
        // 测试空白输入
        String result = PostWordCountUtil.extractText("   ");
        assertEquals("", result);
    }

    @Test
    void testExtractTextBasicHtml() {
        // 测试基本 HTML 标签移除
        String html = "<p>Hello <strong>world</strong>!</p>";
        String result = PostWordCountUtil.extractText(html);
        assertEquals(" Hello  world ! ", result);
    }

    @Test
    void testExtractTextScriptAndStyle() {
        // 测试 script 和 style 标签移除
        String html = "<html><head><style>body{color:red;}</style></head>"
            + "<body><script>alert('test');</script><p>Content</p></body></html>";
        String result = PostWordCountUtil.extractText(html);
        assertEquals("       Content   ", result);
    }

    @Test
    void extractTextShouldStripHtmlAndCollapseWhitespace() {
        String html = "<p>Hello <b>world</b>!<script>alert(1)</script>\n<style>p{}</style></p>";
        String text = PostWordCountUtil.extractText(html);
        assertEquals(" Hello  world ! \n" + "  ", text);
    }

    @Test
    void testExtractTextEntities() {
        // 测试 HTML 实体处理
        String html = "<p>Hello&nbsp;world&nbsp;test</p>";
        String result = PostWordCountUtil.extractText(html);
        assertEquals(" Hello world test ", result);
    }

    @Test
    void testExtractTextWhitespaceNormalization() {
        // 测试空白字符规范化
        String html = "<p>Hello\n\t  world\r\n!</p>";
        String result = PostWordCountUtil.extractText(html);
        assertEquals(" Hello\n\t  world\r\n! ", result);
    }

    @Test
    void testExtractTextPunctuationSpacing() {
        // 测试标点符号前空格处理
        String html = "<p>Hello , world .</p><p>你好 ，世界 。</p>";
        String result = PostWordCountUtil.extractText(html);
        assertEquals(" Hello , world .  你好 ，世界 。 ", result);
    }

    @Test
    void testCountPlainTextWordsNullAndEmpty() {
        // 测试 null 和空字符串
        assertEquals(BigInteger.ZERO, PostWordCountUtil.countPlainTextWords(null));
        assertEquals(BigInteger.ZERO, PostWordCountUtil.countPlainTextWords(""));
    }

    @Test
    void safeCountAsciiWords() {
        // 测试英文单词计数
        assertEquals(BigInteger.valueOf(3),
            PostWordCountUtil.countPlainTextWords("Hello world test"));
        assertEquals(BigInteger.valueOf(4),
            PostWordCountUtil.countPlainTextWords("Hello123 world-test abc"));
        assertEquals(BigInteger.valueOf(2), PostWordCountUtil.countPlainTextWords("Hello   world"));
        assertEquals(BigInteger.valueOf(2), PostWordCountUtil.countPlainTextWords("Hello world!"));
        assertEquals(BigInteger.valueOf(3), PostWordCountUtil.countPlainTextWords("Hello, Halo 2"));
        assertEquals(BigInteger.valueOf(2), PostWordCountUtil.countPlainTextWords("10.11"));
    }

    @Test
    void safeCountCjkCharacters() {
        // 测试中日韩字符计数
        assertEquals(BigInteger.valueOf(4), PostWordCountUtil.countPlainTextWords("你好，世界！"));
        assertEquals(BigInteger.valueOf(5), PostWordCountUtil.countPlainTextWords("こんにちは"));
        assertEquals(BigInteger.valueOf(6), PostWordCountUtil.countPlainTextWords("안녕하십니까"));
    }

    @Test
    void testCountPlainTextWordsMixedContent() {
        // 测试混合内容
        assertEquals(BigInteger.valueOf(4),
            PostWordCountUtil.countPlainTextWords("Hello 你好 world"));
        assertEquals(BigInteger.valueOf(6),
            PostWordCountUtil.countPlainTextWords("test123 测试 hello 世界"));
        assertEquals(BigInteger.valueOf(4),
            PostWordCountUtil.countPlainTextWords("Hello,你好！world"));
        assertEquals(BigInteger.valueOf(1),
            PostWordCountUtil.countPlainTextWords("Hello123123test"));
    }

    @Test
    void testCountPlainTextWordsUnicodeNormalization() {
        // 测试 Unicode 标准化
        String combined = "A\u030A"; // A + 环形符 = Å
        String single = "\u00C5";    // 直接的 Å

        // 标准化后应该得到相同的字数
        assertEquals(BigInteger.ONE, PostWordCountUtil.countPlainTextWords(combined));
        assertEquals(BigInteger.ONE, PostWordCountUtil.countPlainTextWords(single));
    }

    @Test
    void testCountPlainTextWordsSpecialCharacters() {
        // 测试特殊字符和标点符号
        assertEquals(BigInteger.ZERO, PostWordCountUtil.countPlainTextWords("\u00A0\t\n\n"));
        assertEquals(BigInteger.ZERO, PostWordCountUtil.countPlainTextWords("!!@#$%^&*()"));
        assertEquals(BigInteger.ZERO, PostWordCountUtil.countPlainTextWords("😂🤣😍❤️🙌👌"));
    }
}
