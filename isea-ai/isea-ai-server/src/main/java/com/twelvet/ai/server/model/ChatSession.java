package com.twelvet.ai.server.model;

import lombok.Data;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

/**
 * ChatSession
 * 会话管理
 */
@Data
public class ChatSession {
    private String sessionId;
    private String userId;
    private LocalDateTime createTime;
    private LocalDateTime modifiedTime;
    private String modelType;
    private ChatMemory chatMemory;  // 这里是 ChatMemory
    private String sessionState;
    private boolean isDeleted;
    private Duration sessionTimeout;
    private double temperature;
    private int maxResponseLength;
    private String sessionSummary;
    private String conversationTone;
    private List<String> loadedPlugins;
    private HashMap<String, String> messageIdentifiers;

    // 构造函数初始化 ChatMemory
    @Autowired
    public ChatSession(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;  // 自动注入 ChatMemory
    }

    // 可以定义其他构造方法，确保默认值和会话创建逻辑
    public ChatSession(String sessionId, String userId) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.createTime = LocalDateTime.now();
        this.modifiedTime = LocalDateTime.now();
        this.sessionState = "ACTIVE";
        this.isDeleted = false;
    }
}
