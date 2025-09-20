package top.howiehz.halopluginextraapi.finder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ExtraApiFinderImplTest {

    @Test
    void extractTextShouldStripHtmlAndCollapseWhitespace() {
        String html = "<p>Hello <b>world</b>!<script>alert(1)</script>\n<style>p{}</style></p>";
        String text = ExtraApiFinderImpl.extractText(html);
        assertEquals("Hello world!", text);
    }

    @Test
    void safeCountAsciiWords() {
        assertEquals(2, ExtraApiFinderImpl.safeCount("Hello world!"));
        assertEquals(3, ExtraApiFinderImpl.safeCount("Hello, Halo 2"));
    }

    @Test
    void safeCountCjkCharacters() {
        assertEquals(4, ExtraApiFinderImpl.safeCount("你好，世界！"));
        assertEquals(5, ExtraApiFinderImpl.safeCount("你好 Hello 世界"));
    }
}

