package com.itheima.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryRewriteService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    /**
     * 结合对话历史改写查询，补充省略的上下文
     */
    public String rewrite(String originalQuery, List<Message> history) {
        // 没有历史记录时直接返回原查询
        if (history == null || history.isEmpty()) {
            return originalQuery;
        }
        // 只取最近的 3 轮对话
        List<Message> recent = history.size() > 6 ? history.subList(history.size() - 6, history.size()) : history;
        String historyStr = recent.stream()
                .map(m -> m.getMessageType().name() + ": " + m.getText())
                .collect(Collectors.joining("\n"));

        String prompt = """
                你是一个查询改写助手。给定对话历史和用户当前的问题，如果当前问题中存在代词、省略等上下文依赖，请将其改写为完整、独立、明确的查询。如果没有省略，直接返回原问题。
                
                对话历史：
                %s
                
                当前问题：%s
                
                改写后的查询（仅输出改写结果，不要额外文字）：
                """.formatted(historyStr, originalQuery);

        try {
            String rewritten = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            String result = rewritten != null ? rewritten.trim() : originalQuery;
            if (!result.equals(originalQuery)) {
                log.info("Query 改写: '{}' → '{}'", originalQuery, result);
            }
            return result;
        } catch (Exception e) {
            log.warn("Query 改写失败，使用原始查询", e);
            return originalQuery;
        }
    }

    /**
     * 查询扩展：生成多个语义变体提高召回率
     */
    public List<String> expand(String originalQuery) {
        String prompt = """
                你是一个查询扩展助手。请将以下查询扩展为 3 个不同表述的变体，每个变体保留原意但使用不同的措辞。
                以 JSON 数组格式输出，例如：["变体1", "变体2", "变体3"]
                
                原始查询：%s
                """.formatted(originalQuery);

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            List<String> variants = objectMapper.readValue(response, new TypeReference<>() {});
            log.info("Query 扩展: '{}' → {} 个变体", originalQuery, variants.size());
            return variants;
        } catch (Exception e) {
            log.warn("Query 扩展失败", e);
            return List.of(originalQuery);
        }
    }

    /**
     * HyDE（假设性文档嵌入）：先生成假设性回答，再用其向量检索
     */
    public String hyde(String originalQuery) {
        String prompt = """
                用户的问题是：%s
                
                请根据你对相关领域知识的了解，写一段假设性的回答文本。这段文本应该看起来像一份教科书或文档中针对该问题的标准解释。
                不要提到"假设"字样，直接以客观、专业的语气撰写。
                """.formatted(originalQuery);

        try {
            String hypothetical = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            log.info("HyDE 生成完成，长度: {} 字符", hypothetical != null ? hypothetical.length() : 0);
            return hypothetical;
        } catch (Exception e) {
            log.warn("HyDE 生成失败，使用原始查询", e);
            return originalQuery;
        }
    }
}