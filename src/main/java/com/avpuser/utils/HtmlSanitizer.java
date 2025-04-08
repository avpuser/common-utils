package com.avpuser.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;

public class HtmlSanitizer {

    /**
     * Экранирует HTML-спецсимволы (например, &lt;, &gt;, &amp;) в XHTML-стиле.
     */
    public static String escapeHtml(String html) {
        if (html == null) {
            return null;
        }
        Document document = Jsoup.parseBodyFragment(html);
        document.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
        document.outputSettings().prettyPrint(false); // не форматируем
        return document.body().html();
    }

    /**
     * Удаляет все HTML-теги и возвращает только чистый текст.
     */
    public static String stripHtml(String html) {
        if (html == null) {
            return null;
        }
        return Jsoup.parse(html).text();
    }
}
