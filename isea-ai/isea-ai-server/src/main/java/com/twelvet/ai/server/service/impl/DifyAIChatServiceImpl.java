package com.twelvet.ai.server.service.impl;

import java.time.LocalDate;

import com.twelvet.ai.server.model.ChatSession;
import com.twelvet.ai.server.service.ChatSessionService;
import com.twelvet.ai.server.service.DifyAIChatService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatOptions;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * @author rainyak
 * @Description: Dify OUC AI对话
 */
@Service
@Slf4j
public class DifyAIChatServiceImpl implements DifyAIChatService {
	private final ChatClient openAiChatClient;

	@Resource
	private ChatSessionService chatSessionService;

	public DifyAIChatServiceImpl(ChatClient.Builder modelBuilder, ChatMemory chatMemory) {

		this.openAiChatClient = modelBuilder
				.defaultSystem("""
						您是“中国海洋大学”的聊天支持代理。请以友好、乐于助人且愉快的方式来回复。
						您正在通过在线聊天系统与客户互动。
					   请讲中文。
					   今天的日期是 {current_date}.
					""")
				.defaultOptions(
						OpenAiChatOptions.builder()
								.withTopP(0.7)
								.build()
				)
				.defaultAdvisors(
						new PromptChatMemoryAdvisor(chatMemory)) // Chat Memory
				.build();
	}

	@Override
	public String simpleChat(String sessionId, String userMessageContent) {
		log.info("DifyAIChatServiceImpl.simpleChat start: sessionId {} userMessageContent {}", sessionId, userMessageContent);
		return this.openAiChatClient.prompt()
				.system(s -> s.param("current_date", LocalDate.now().toString()))
				.user(userMessageContent)
				.advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId).param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
				.call()
				.content();
	}

	@Override
	public Flux<String> streamChat(String sessionId, String userMessageContent) {
		log.info("DifyAIChatServiceImpl.streamChat start: sessionId {} userMessageContent {}", sessionId, userMessageContent);
		return this.openAiChatClient.prompt()
				.system(s -> s.param("current_date", LocalDate.now().toString()))
				.user(userMessageContent)
				.advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId).param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
				.stream()
				.content();
	}
}
