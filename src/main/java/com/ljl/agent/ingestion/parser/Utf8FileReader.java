package com.ljl.agent.ingestion.parser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class Utf8FileReader {

    private Utf8FileReader() {
    }

    static String read(Path path) {
        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        StringBuilder text = new StringBuilder();
        try (Reader reader = new InputStreamReader(
                Files.newInputStream(path),
                decoder
        )) {
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                text.append(buffer, 0, read);
            }
            return text.toString();
        } catch (IOException exception) {
            throw new DocumentParsingException(
                    "文本文件无法按 UTF-8 读取",
                    exception
            );
        }
    }
}
