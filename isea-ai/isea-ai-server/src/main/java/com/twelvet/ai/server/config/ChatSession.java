package com.twelvet.ai.server.config;

import lombok.Getter;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.memory.ChatMemory;

/**
 * ChatSession
 * 会话管理
 */
@Service
public class ChatSession {

    private final ChatMemory defaultChatMemory;

    /**
     * 默认自动装配一个 ChatMemory 实例
     * @param chatMemory
     */
    @Autowired
    public ChatSession(ChatMemory chatMemory) {
        this.defaultChatMemory = chatMemory;
    }

    public ChatMemory createNewSession() {
        // 创建并返回新的 ChatMemory 实例
        return new InMemoryChatMemory();
    }

}