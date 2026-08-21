package com.ljl.agent.ingestion.parser;

import com.ljl.agent.ingestion.DocumentFileType;

import java.nio.file.Path;

public interface DocumentParser {

    boolean supports(DocumentFileType fileType);

    String parse(Path path);
}
