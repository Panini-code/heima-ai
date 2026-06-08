package com.itheima.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.ai.dto.AgentEvent;
import com.itheima.ai.tools.PdfSearchTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.*;

/**
 * 简化 ReAct Agent：单次调用，捕获模型自然输出的思考与工具调用
 */
@Service
public class ReActAgentService {

    private static final Logger log = LoggerFactory.getLogger(ReActAgentService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final PdfSearchTool pdfSearchTool;
    private final ChatMemory chatMemory;

    private final String apiKey;
    private final String baseUrl;
    private final String model;

    private static final int MAX_ITERATIONS = 5;

    public ReActAgentService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            PdfSearchTool pdfSearchTool,
            ChatMemory chatMemory,
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${spring.ai.openai.base-url:}") String baseUrl,
            @Value("${spring.ai.openai.chat.options.model:qwen3.5-omni-plus}") String model) {
        this.objectMapper = objectMapper;
        this.pdfSearchTool = pdfSearchTool;
        this.chatMemory = chatMemory;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.restClient = restClientBuilder.build();
    }

    public Flux<AgentEvent> pdfAgentChat(String prompt, String chatId) {
        return Flux.create((FluxSink<AgentEvent> emitter) -> {
            try {
                // 构建消息（系统 + 历史 + 当前问题）
                List<Map<String, Object>> messages = new ArrayList<>();

                Map<String, Object> systemMsg = new LinkedHashMap<>();
                systemMsg.put("role", "system");
                systemMsg.put("content",
                    "你是小团团，智能 PDF 文档分析助手。\n" +
                    "如果用户的问题需要查阅文档内容，请调用 searchPdf 工具搜索相关段落。\n" +
                    "基于搜索结果给出详细准确的回答，并注明来源文件名。\n" +
                    "用热情详细的语气回答，不要自我介绍。"
                );
                messages.add(systemMsg);

                // 对话历史
                List<Message> history = chatMemory.get(chatId, 10);
                for (Message msg : history) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    if (msg instanceof UserMessage) {
                        m.put("role", "user");
                        m.put("content", msg.getText());
                    } else if (msg instanceof AssistantMessage) {
                        m.put("role", "assistant");
                        m.put("content", msg.getText());
                    }
                    if (!m.isEmpty()) messages.add(m);
                }

                Map<String, Object> userMsg = new LinkedHashMap<>();
                userMsg.put("role", "user");
                userMsg.put("content", prompt);
                messages.add(userMsg);

                // 工具定义
                List<Map<String, Object>> tools = buildPdfToolDefinitions();

                // ReAct 循环（单次调用，自然捕获模型输出）
                boolean hasAnswer = false;
                for (int step = 0; step < MAX_ITERATIONS; step++) {
                    Map<String, Object> req = new LinkedHashMap<>();
                    req.put("model", model);
                    req.put("messages", messages);
                    req.put("tools", tools);
                    req.put("temperature", 0.5);

                    String respBody = restClient.post()
                            .uri(baseUrl + "/v1/chat/completions")
                            .header("Authorization", "Bearer " + apiKey)
                            .header("Content-Type", "application/json")
                            .body(req)
                            .retrieve()
                            .body(String.class);

                    Map<String, Object> resp = objectMapper.readValue(respBody, Map.class);
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
                    if (choices == null || choices.isEmpty()) {
                        emitter.next(new AgentEvent("error", "模型返回为空", null, null, null));
                        break;
                    }

                    Map<String, Object> msgData = (Map<String, Object>) choices.get(0).get("message");
                    if (msgData == null) {
                        emitter.next(new AgentEvent("error", "响应格式异常", null, null, null));
                        break;
                    }

                    String content = (String) msgData.get("content");
                    List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) msgData.get("tool_calls");

                    // 构建 assistant 消息
                    Map<String, Object> asstMsg = new LinkedHashMap<>();
                    asstMsg.put("role", "assistant");
                    asstMsg.put("content", content);

                    if (toolCalls != null && !toolCalls.isEmpty()) {
                        asstMsg.put("tool_calls", toolCalls);
                        messages.add(asstMsg);

                        for (Map<String, Object> tc : toolCalls) {
                            Map<String, Object> func = (Map<String, Object>) tc.get("function");
                            String funcName = (String) func.get("name");
                            String funcArgs = (String) func.get("arguments");
                            String toolCallId = (String) tc.get("id");

                            emitter.next(new AgentEvent("tool_call", null, funcName, funcArgs, null));

                            String result = executePdfTool(funcName, funcArgs, chatId);

                            emitter.next(new AgentEvent("tool_result", null, funcName, null, result));

                            Map<String, Object> toolResultMsg = new LinkedHashMap<>();
                            toolResultMsg.put("role", "tool");
                            toolResultMsg.put("tool_call_id", toolCallId);
                            toolResultMsg.put("content", result);
                            messages.add(toolResultMsg);
                        }
                    } else {
                        messages.add(asstMsg);
                        emitter.next(new AgentEvent("answer", content != null ? content : "", null, null, null));
                        hasAnswer = true;
                        break;
                    }
                }

                // 持久化
                if (hasAnswer) {
                    try {
                        List<Message> memMsgs = new ArrayList<>();
                        memMsgs.add(new UserMessage(prompt));
                        for (int i = messages.size() - 1; i >= 0; i--) {
                            Map<String, Object> m = messages.get(i);
                            if ("assistant".equals(m.get("role"))) {
                                String c = (String) m.get("content");
                                if (c != null && !c.isEmpty()) {
                                    memMsgs.add(new AssistantMessage(c));
                                    break;
                                }
                            }
                        }
                        chatMemory.add(chatId, memMsgs);
                    } catch (Exception memErr) {
                        log.warn("保存对话失败", memErr);
                    }
                }

                if (!hasAnswer) {
                    emitter.next(new AgentEvent("error", "Agent 处理超限，请重试", null, null, null));
                }
                emitter.complete();

            } catch (Exception e) {
                log.error("ReAct Agent 异常", e);
                emitter.next(new AgentEvent("error", e.getMessage(), null, null, null));
                emitter.complete();
            }
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    private List<Map<String, Object>> buildPdfToolDefinitions() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("query", Map.of("type", "string", "description", "搜索关键词"));
        properties.put("fileName", Map.of("type", "string", "description", "文件名（可选）"));

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("type", "object");
        params.put("properties", properties);
        params.put("required", List.of("query"));

        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", "searchPdf");
        function.put("description", "搜索PDF文档内容，根据关键词查找相关文档片段");
        function.put("parameters", params);

        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");
        tool.put("function", function);
        return List.of(tool);
    }

    private String executePdfTool(String funcName, String funcArgs, String chatId) {
        try {
            if ("searchPdf".equals(funcName)) {
                Map<String, Object> args = objectMapper.readValue(funcArgs, Map.class);
                return pdfSearchTool.searchPdf(
                    (String) args.getOrDefault("query", ""),
                    (String) args.get("fileName"),
                    chatId
                );
            }
            return "未知工具: " + funcName;
        } catch (Exception e) {
            log.error("工具执行失败: {}", funcName, e);
            return "工具执行出错: " + e.getMessage();
        }
    }
}