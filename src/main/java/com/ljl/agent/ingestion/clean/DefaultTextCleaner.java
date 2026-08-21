package com.ljl.agent.ingestion.clean;

import com.ljl.agent.ingestion.parser.DocumentParsingException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 只做格式归一化，保留 Markdown 标题、列表、代码块和换行语义。
 */
@Component
public class DefaultTextCleaner implements TextCleaner {

    @Override
    public String clean(String text) {
        if (text == null) {
            throw emptyDocument();
        }

        String normalized = text
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\uFEFF", "")
                .replace("\0", "");

        String[] rawLines = normalized.split("\n", -1);
        List<String> lines = new ArrayList<>(rawLines.length);
        int consecutiveBlankLines = 0;
        for (String rawLine : rawLines) {
            String line = rawLine.replaceFirst("[ \\t]+$", "");
            if (line.isBlank()) {
                consecutiveBlankLines++;
                if (consecutiveBlankLines <= 2) {
                    lines.add("");
                }
            } else {
                consecutiveBlankLines = 0;
                lines.add(line);
            }
        }

        int start = 0;
        int end = lines.size();
        while (start < end && lines.get(start).isBlank()) {
            start++;
        }
        while (end > start && lines.get(end - 1).isBlank()) {
            end--;
        }
        if (start == end) {
            throw emptyDocument();
        }
        return String.join("\n", lines.subList(start, end));
    }

    private DocumentParsingException emptyDocument() {
        return new DocumentParsingException("文档解析后没有可用文本");
    }
}
