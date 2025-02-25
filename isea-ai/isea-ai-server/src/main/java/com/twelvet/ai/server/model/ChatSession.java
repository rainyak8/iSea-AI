package com.twelvet.ai.server.model;

import com.alibaba.fastjson2.JSON;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
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
@Slf4j
public class ChatSession {
    private String sessionId;
    private Long userId;
    private LocalDateTime createTime;
    private LocalDateTime modifiedTime;
    private String modelType;
    private SessionChatMemory chatMemory;
    private String sessionState;
    private boolean isDeleted;
    private Duration sessionTimeout;
    private double temperature;
    private int maxResponseLength;
    private String sessionSummary;
    private String conversationTone;
    private List<String> loadedPlugins;
    private HashMap<String, String> messageIdentifiers;

    // 可以定义其他构造方法，确保默认值和会话创建逻辑
    public ChatSession(String sessionId, Long userId) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.createTime = LocalDateTime.now();
        this.modifiedTime = LocalDateTime.now();
        this.sessionState = "ACTIVE";
        this.isDeleted = false;
        SessionChatMemory sessionChatMemory = new SessionChatMemory();
        sessionChatMemory.add(new SystemMessage("agent"));
        this.chatMemory = sessionChatMemory;  // 自动注入 ChatMemory 并序列化
    }
}
