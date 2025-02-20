package com.twelvet.ai.server.service.impl;

import org.springframework.ai.chat.client.ChatClient;
import com.twelvet.ai.server.service.DifyAIChatService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * @author rainyak
 * @Description: Dify OUC AI对话
 */
@Service
public class DifyAIChatServiceImpl implements DifyAIChatService {

	private static final Logger log = LoggerFactory.getLogger(DifyAIChatServiceImpl.class);

	private final ChatClient chatClient;

	public DifyAIChatServiceImpl(ChatClient.Builder modelBuilder, VectorStore vectorStore, ChatMemory chatMemory) {

		this.chatClient = modelBuilder
				.defaultSystem("""
						您是“中国海洋大学”的聊天支持代理。请以友好、乐于助人且愉快的方式来回复。
						您正在通过在线聊天系统与客户互动。
					   请讲中文。
					   今天的日期是 {current_date}.
					""")
				.defaultAdvisors(
						new PromptChatMemoryAdvisor(chatMemory)) // Chat Memory
				.defaultFunctions("介绍中国海洋大学")
				.build();
	}

	@Override
	public String simpleChat(String chatId, String userMessageContent) {
		log.info("DifyAIChatServiceImpl.simpleChat start: chatId {} userMessageContent {}", chatId, userMessageContent);
		return this.chatClient.prompt()
				.system(s -> s.param("current_date", LocalDate.now().toString()))
				.user(userMessageContent)
				.advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId).param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
				.call()
				.content();
	}

	@Override
	public Flux<String> streamChat(String chatId, String userMessageContent) {
		log.info("DifyAIChatServiceImpl.streamChat start: chatId {} userMessageContent {}", chatId, userMessageContent);
		return this.chatClient.prompt()
				.system(s -> s.param("current_date", LocalDate.now().toString()))
				.user(userMessageContent)
				.advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId).param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
				.stream()
				.content();
	}

}
