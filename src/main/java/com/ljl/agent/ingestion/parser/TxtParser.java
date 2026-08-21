package com.ljl.agent.ingestion.parser;

import com.ljl.agent.ingestion.DocumentFileType;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class TxtParser implements DocumentParser {

    @Override
    public boolean supports(DocumentFileType fileType) {
        return fileType == DocumentFileType.TXT;
    }

    @Override
    public String parse(Path path) {
        return Utf8FileReader.read(path);
    }
}
