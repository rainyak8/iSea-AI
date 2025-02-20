package com.twelvet.ai.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
/**
 * ChatMemory
 * 聊天记录
 */
@Configuration
public class ChatMemoryConfig {

    @Bean
    public ChatMemory chatMemory() {
        // 创建并返回 ChatMemory 实例
        return new InMemoryChatMemory();
    }
}