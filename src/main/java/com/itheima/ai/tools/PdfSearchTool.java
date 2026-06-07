package com.itheima.ai.tools;

import com.itheima.ai.repository.FileRepository;
import com.itheima.ai.service.ReRankerService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import java.util.List;

@RequiredArgsConstructor
@Component
public class PdfSearchTool {

    private static final Logger log = LoggerFactory.getLogger(PdfSearchTool.class);

    private final VectorStore vectorStore;
    private final ReRankerService reRankerService;
    private final FileRepository fileRepository;

    @Tool(description = "搜索PDF文档内容，根据用户的问题查找相关文档片段")
    public String searchPdf(
            @ToolParam(description = "用户的问题或搜索关键词") String query,
            @ToolParam(description = "指定要搜索的文件名", required = false) String fileName) {

        log.info("Agent PDF搜索: query='{}' fileName='{}'", query, fileName);

        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(8)
                .similarityThreshold(0.4);

        if (fileName != null && !fileName.isEmpty()) {
            builder.filterExpression("file_name == '" + fileName + "'");
        }

        List<Document> retrievedDocs = vectorStore.similaritySearch(builder.build());
        log.info("搜索结果: 获取到 {} 个文档片段", retrievedDocs.size());

        if (retrievedDocs.isEmpty()) {
            return "未找到相关文档内容。";
        }

        List<Document> rerankedDocs = reRankerService.rerank(query, retrievedDocs, 4);

        StringBuilder result = new StringBuilder();
        result.append("找到以下相关文档内容：\n\n");
        for (int i = 0; i < rerankedDocs.size(); i++) {
            Document doc = rerankedDocs.get(i);
            String source = doc.getMetadata() != null
                    ? doc.getMetadata().getOrDefault("file_name", "未知文件").toString()
                    : "未知文件";
            result.append("--- 片段 ").append(i + 1).append("（来源：").append(source).append("）---\n");
            result.append(doc.getText()).append("\n\n");
        }

        return result.toString();
    }
}
