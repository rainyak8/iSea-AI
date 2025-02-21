package com.twelvet.ai.server.model;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.context.annotation.Scope;

/**
 * ChatMemoryConfig
 * 配置 ChatMemory Bean
 */
@Configuration
public class ChatMemoryConfig {

    /**
     * 配置 ChatMemory Bean，作用域为 prototype，每次获取都会返回一个新的实例
     * @return 新的 ChatMemory 实例
     */
    @Bean
    @Scope("prototype")
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();  // 返回一个新的 InMemoryChatMemory 实例
    }
}