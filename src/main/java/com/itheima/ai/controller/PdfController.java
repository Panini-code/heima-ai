package com.itheima.ai.controller;

import com.itheima.ai.entity.vo.Result;
import com.itheima.ai.repository.ChatHistoryRepository;
import com.itheima.ai.repository.FileRepository;
import com.itheima.ai.service.DocumentChunkingService;
import com.itheima.ai.service.QueryRewriteService;
import com.itheima.ai.service.ReRankerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/pdf")
public class PdfController {

    private final FileRepository fileRepository;
    private final VectorStore vectorStore;
    private final ChatClient pdfChatClient;
    private final ChatHistoryRepository chatHistoryRepository;
    private final DocumentChunkingService chunkingService;
    private final ReRankerService reRankerService;
    private final QueryRewriteService queryRewriteService;
    private final ChatMemory chatMemory;
    private final StringRedisTemplate redisTemplate;

    private static final String VECTOR_INDEX_KEY = "chat:pdf:vectors:";

    @RequestMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(String prompt, String chatId, @RequestParam(required = false) String fileName) {
        List<String> files = fileRepository.listFiles(chatId);
        if (files.isEmpty()) {
            throw new RuntimeException("会话文件不存在！");
        }
        chatHistoryRepository.save("pdf", chatId);

        // 使用指定的文件名过滤，如果未指定则使用第一个文件
        String targetFile = (fileName != null && files.contains(fileName)) ? fileName : files.get(0);

        List<Message> history = chatMemory.get(chatId, 10);
        String rewrittenQuery = queryRewriteService.rewrite(prompt, history);

        SearchRequest searchRequest = SearchRequest.builder()
                .query(rewrittenQuery)
                .filterExpression("file_name == '" + targetFile + "'")
                .topK(5)
                .similarityThreshold(0.5)
                .build();
        List<Document> retrievedDocs = vectorStore.similaritySearch(searchRequest);

        List<Document> rerankedDocs = reRankerService.rerank(rewrittenQuery, retrievedDocs, 3);

        String context = rerankedDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        log.info("PDF Chat: query='{}' rewritten='{}' file='{}' retrieved={} reranked={}",
                prompt, rewrittenQuery, targetFile, retrievedDocs.size(), rerankedDocs.size());

        String systemPrompt = "请根据上下文回答问题，遇到上下文没有的问题，不要随意编造。\n\n上下文：\n" + context;
        return pdfChatClient.prompt()
                .system(systemPrompt)
                .user(prompt)
                .stream()
                .content();
    }

    @RequestMapping("/upload/{chatId}")
    public Result uploadPdf(@PathVariable String chatId, @RequestParam("file") MultipartFile file) {
        long startTime = System.currentTimeMillis();
        try {
            if (!Objects.equals(file.getContentType(), "application/pdf")) {
                return Result.fail("只能上传PDF文件！");
            }
            log.info("═══════ 开始上传PDF: {} ═══════", file.getOriginalFilename());
            boolean success = fileRepository.save(chatId, file.getResource());
            if (!success) {
                return Result.fail("保存文件失败！");
            }
            this.writeToVectorStore(file.getResource());
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("═══════ PDF上传完成（总耗时: {}秒）═══════", elapsed / 1000);
            return Result.ok();
        } catch (Exception e) {
            log.error("上传PDF失败", e);
            return Result.fail("上传文件失败！");
        }
    }

    @GetMapping("/files/{chatId}")
    public Result listFiles(@PathVariable String chatId) {
        List<String> files = fileRepository.listFiles(chatId);
        return Result.ok(files);
    }

    @DeleteMapping("/file/{chatId}")
    public Result deleteFile(@PathVariable String chatId, @RequestParam String filename) {
        try {
            // 删除向量库中对应文档
            deleteVectorsByFileName(filename);
            // 删除文件记录
            fileRepository.deleteFile(chatId, filename);
            log.info("已删除文件及向量: chatId={}, filename={}", chatId, filename);
            return Result.ok();
        } catch (Exception e) {
            log.error("删除文件失败", e);
            return Result.fail("删除文件失败！");
        }
    }

    @GetMapping("/file/{chatId}")
    public ResponseEntity<Resource> download(@PathVariable("chatId") String chatId,
                                              @RequestParam(required = false) String fileName) throws IOException {
        Resource resource;
        if (fileName != null) {
            resource = fileRepository.getFile(chatId, fileName);
        } else {
            resource = fileRepository.getFile(chatId);
        }
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        String filename = URLEncoder.encode(Objects.requireNonNull(resource.getFilename()), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    private void writeToVectorStore(Resource resource) {
        String filename = resource.getFilename();
        long t0 = System.currentTimeMillis();

        log.info("┌─────────────────────────────────────────┐");
        log.info("│ ① 读取PDF...                            │");
        log.info("└─────────────────────────────────────────┘");
        PagePdfDocumentReader reader = new PagePdfDocumentReader(
                resource,
                PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
                        .withPagesPerDocument(1)
                        .build()
        );
        List<Document> pageDocs = reader.read();
        String fullText = pageDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));
        log.info("   PDF读取完成: {} 页, {} 字符, 耗时 {}ms", pageDocs.size(), fullText.length(), System.currentTimeMillis() - t0);

        long t1 = System.currentTimeMillis();
        String strategy = chunkingService.getChunkProperties().getStrategy();
        List<DocumentChunkingService.DocumentChunk> chunks;
        if ("paragraph".equalsIgnoreCase(strategy)) {
            chunks = chunkingService.chunkByParagraph(fullText, filename);
        } else {
            chunks = chunkingService.chunkBySize(fullText, filename);
        }
        log.info("   分块完成: {} 个chunk（策略={}）, 耗时 {}ms", chunks.size(), strategy, System.currentTimeMillis() - t1);

        List<Document> documents = chunks.stream()
                .map(chunk -> {
                    Document doc = new Document(chunk.getText(), chunk.getMetadata());
                    doc.getMetadata().put("file_name", filename);
                    doc.getMetadata().put("chunk_index", chunk.getChunkIndex());
                    doc.getMetadata().put("chunk_total", chunks.size());
                    return doc;
                })
                .toList();

        // 清除旧向量
        deleteVectorsByFileName(filename);

        log.info("┌─────────────────────────────────────────┐");
        log.info("│ ② 开始写入向量库（调用嵌入API生成向量）  │");
        log.info("│    共 {} 个chunk, 预计约 {} 秒          │", documents.size(), documents.size() * 1);
        log.info("└─────────────────────────────────────────┘");

        int batchSize = 5;
        int totalBatches = (documents.size() + batchSize - 1) / batchSize;
        for (int i = 0; i < documents.size(); i += batchSize) {
            int end = Math.min(i + batchSize, documents.size());
            List<Document> batch = documents.subList(i, end);
            long bt0 = System.currentTimeMillis();

            vectorStore.add(batch);

            String vectorKey = VECTOR_INDEX_KEY + filename;
            batch.forEach(doc -> redisTemplate.opsForSet().add(vectorKey, doc.getId()));

            long bt = System.currentTimeMillis() - bt0;
            int batchNum = i / batchSize + 1;
            log.info("   批次[{}/{}]: chunk[{}-{}] 写入完成, 耗时 {}ms (嵌入API调用中...)",
                    batchNum, totalBatches, i, end - 1, bt);
        }

        long totalTime = System.currentTimeMillis() - t0;
        log.info("③ 写入完成！共 {} 个chunk, 总耗时 {} 秒", documents.size(), totalTime / 1000);
    }

    private void deleteVectorsByFileName(String filename) {
        String vectorKey = VECTOR_INDEX_KEY + filename;
        Set<String> docIds = redisTemplate.opsForSet().members(vectorKey);
        if (docIds != null && !docIds.isEmpty()) {
            vectorStore.delete(List.copyOf(docIds));
            redisTemplate.delete(vectorKey);
            log.info("已删除 {} 个旧向量文档", docIds.size());
        }
    }
}
