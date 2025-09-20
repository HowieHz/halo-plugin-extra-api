package top.howiehz.halopluginextraapi.finder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WordCountFinderImplTest {

    @Test
    void extractTextShouldStripHtmlAndCollapseWhitespace() {
        String html = "<p>Hello <b>world</b>!<script>alert(1)</script>\n<style>p{}</style></p>";
        String text = WordCountFinderImpl.extractText(html);
        assertEquals("Hello world!", text);
    }

    @Test
    void safeCountAsciiWords() {
        assertEquals(2, WordCountFinderImpl.safeCount("Hello world!"));
        assertEquals(3, WordCountFinderImpl.safeCount("Hello, Halo 2"));
    }

    @Test
    void safeCountCjkCharacters() {
        assertEquals(4, WordCountFinderImpl.safeCount("你好，世界！"));
        assertEquals(5, WordCountFinderImpl.safeCount("你好 Hello 世界"));
    }
}

