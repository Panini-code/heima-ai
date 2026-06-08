package com.itheima.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 思考链事件 DTO，用于 SSE 流式传输
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentEvent {
    /** 事件类型: reasoning(思考), tool_call(调用工具), tool_result(工具结果), answer(回答), error(错误) */
    private String type;
    /** 文本内容（思考或回答的文本） */
    private String content;
    /** 工具名称 */
    private String toolName;
    /** 工具输入参数（JSON 字符串） */
    private String toolInput;
    /** 工具输出结果 */
    private String toolOutput;
}
