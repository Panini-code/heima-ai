package com.itheima.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReRankerService {

    private static final Logger log = LoggerFactory.getLogger(ReRankerService.class);

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    /**
     * 对检索结果进行重排序，返回 TopN 个文档
     */
    public List<Document> rerank(String query, List<Document> documents, int topN) {
        if (documents == null || documents.isEmpty() || documents.size() <= 1) {
            return documents;
        }
        // 1. 构建打分 prompt
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个文档相关性评分专家。请判断以下文档片段与用户问题之间的相关性。\n\n");
        prompt.append("用户问题：").append(query).append("\n\n");
        prompt.append("以下是需要评分的文档片段：\n\n");

        for (int i = 0; i < documents.size(); i++) {
            String text = documents.get(i).getText();
            prompt.append("--- 文档 ").append(i).append(" ---\n");
            prompt.append(text, 0, Math.min(text.length(), 300)).append("\n\n");
        }

        prompt.append("请以JSON格式输出每个文档的相关性评分（0-10分，10分最相关），格式如下：\n");
        prompt.append("{\"scores\": [{\"index\": 0, \"score\": 8}, {\"index\": 1, \"score\": 3}]}\n");
        prompt.append("仅输出JSON，不要额外文字。");

        try {
            // 2. 调用 LLM 评分
            String response = chatClient.prompt()
                    .user(prompt.toString())
                    .call()
                    .content();

            // 3. 解析评分结果
            Map<String, List<Map<String, Object>>> result = objectMapper.readValue(
                    response, new TypeReference<>() {});

            List<Map<String, Object>> scores = result.get("scores");
            if (scores == null) {
                log.warn("ReRanker 返回格式异常，使用原始排序");
                return documents.subList(0, Math.min(topN, documents.size()));
            }

            // 4. 按评分排序
            Map<Integer, Double> scoreMap = new HashMap<>();
            for (Map<String, Object> s : scores) {
                int idx = ((Number) s.get("index")).intValue();
                double score = ((Number) s.get("score")).doubleValue();
                scoreMap.put(idx, score);
            }

            List<Document> reranked = documents.stream()
                    .sorted(Comparator.comparingDouble(
                            (Document d) -> scoreMap.getOrDefault(documents.indexOf(d), 0.0))
                            .reversed())
                    .limit(topN)
                    .collect(Collectors.toList());

            log.info("ReRanker 完成: 原始 {} 条 → 重排序后 Top {}", documents.size(), reranked.size());
            return reranked;

        } catch (Exception e) {
            log.error("ReRanker 执行异常，使用原始排序", e);
            return documents.subList(0, Math.min(topN, documents.size()));
        }
    }
}