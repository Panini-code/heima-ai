package com.itheima.ai.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.rag.chunk")
public class ChunkProperties {
    /** 每块最大字符数 */
    private int maxSize = 500;
    /** 相邻块之间重叠字符数 */
    private int overlap = 100;
    /** 分块策略: sliding / paragraph */
    private String strategy = "sliding";
}