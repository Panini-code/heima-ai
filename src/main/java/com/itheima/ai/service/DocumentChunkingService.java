package com.itheima.ai.service;

import lombok.Getter;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

@Component
public class DocumentChunkingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentChunkingService.class);

    private final ChunkProperties chunkProperties;

    public DocumentChunkingService(ChunkProperties chunkProperties) {
        this.chunkProperties = chunkProperties;
    }

    public ChunkProperties getChunkProperties() {
        return chunkProperties;
    }

    public List<DocumentChunk> chunkBySize(String text, String fileName) {
        int maxSize = chunkProperties.getMaxSize();
        int overlap = chunkProperties.getOverlap();
        List<DocumentChunk> chunks = new ArrayList<>();
        int start = 0;
        int index = 0;

        while (start < text.length()) {
            int end = Math.min(start + maxSize, text.length());
            if (end < text.length()) {
                int breakPoint = findBreakPoint(text, start, end, maxSize / 2);
                end = breakPoint > start ? breakPoint : end;
            }
            String chunkText = text.substring(start, end).trim();
            if (!chunkText.isEmpty()) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("file_name", fileName);
                metadata.put("chunk_index", index);
                metadata.put("chunk_total", -1);
                metadata.put("start_pos", start);
                chunks.add(new DocumentChunk(chunkText, index, start, metadata));
                index++;
            }
            // 已到达文本末尾，结束循环
            if (end >= text.length()) {
                break;
            }
            start = end - overlap;
            if (start < 0) start = 0;
        }
        int total = chunks.size();
        chunks.forEach(c -> c.getMetadata().put("chunk_total", total));
        log.info("文本分块完成: 共 {} 块, 策略=sliding, maxSize={}, overlap={}", total, maxSize, overlap);
        return chunks;
    }

    public List<DocumentChunk> chunkByParagraph(String text, String fileName) {
        int maxSize = chunkProperties.getMaxSize();
        int overlap = chunkProperties.getOverlap();
        List<DocumentChunk> result = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\s*\\n");
        int globalIndex = 0;

        for (String para : paragraphs) {
            para = para.trim();
            if (para.isEmpty()) continue;
            if (para.length() <= maxSize) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("file_name", fileName);
                metadata.put("chunk_index", globalIndex);
                metadata.put("chunk_total", -1);
                result.add(new DocumentChunk(para, globalIndex, -1, metadata));
                globalIndex++;
            } else {
                int start = 0;
                while (start < para.length()) {
                    int end = Math.min(start + maxSize, para.length());
                    if (end < para.length()) {
                        int bp = findBreakPoint(para, start, end, maxSize / 2);
                        end = bp > start ? bp : end;
                    }
                    String chunkText = para.substring(start, end).trim();
                    if (!chunkText.isEmpty()) {
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("file_name", fileName);
                        metadata.put("chunk_index", globalIndex);
                        metadata.put("chunk_total", -1);
                        result.add(new DocumentChunk(chunkText, globalIndex, start, metadata));
                        globalIndex++;
                    }
                    if (end >= para.length()) {
                        break;
                    }
                    start = end - overlap;
                    if (start < 0) start = 0;
                }
            }
        }
        int total = result.size();
        result.forEach(c -> c.getMetadata().put("chunk_total", total));
        log.info("文本分块完成: 共 {} 块, 策略=paragraph, maxSize={}, overlap={}", total, maxSize, overlap);
        return result;
    }

    private int findBreakPoint(String text, int start, int end, int minLen) {
        for (int i = end - 1; i >= start + minLen; i--) {
            char c = text.charAt(i);
            if (c == '\n') return i + 1;
        }
        for (int i = end - 1; i >= start + minLen; i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '？' || c == '！' || c == '；') return i + 1;
        }
        return end;
    }

    @Getter
    public static class DocumentChunk {
        private final String text;
        private final int chunkIndex;
        private final int startPos;
        private final Map<String, Object> metadata;

        public DocumentChunk(String text, int chunkIndex, int startPos, Map<String, Object> metadata) {
            this.text = text;
            this.chunkIndex = chunkIndex;
            this.startPos = startPos;
            this.metadata = metadata;
        }
    }
}