package com.ljl.agent.ingestion.parser;

import com.ljl.agent.ingestion.DocumentFileType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ParserRegistry {

    private final List<DocumentParser> parsers;

    public ParserRegistry(List<DocumentParser> parsers) {
        this.parsers = List.copyOf(parsers);
    }

    public DocumentParser requireParser(DocumentFileType fileType) {
        return parsers.stream()
                .filter(parser -> parser.supports(fileType))
                .findFirst()
                .orElseThrow(() -> new DocumentParsingException(
                        "当前文件类型没有可用解析器"
                ));
    }
}
