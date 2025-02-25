package com.twelvet.ai.server.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson2.JSON;
import com.twelvet.ai.server.model.ChatMemoryConfig;
import com.twelvet.ai.server.model.ChatSession;
import com.twelvet.ai.server.model.SessionChatMemory;
import com.twelvet.ai.server.service.ChatSessionService;
import com.twelvet.ai.server.service.DifyAIChatService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
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
	public String simpleChat(String sessionId, Long userId, String userMessageContent) {
		log.info("DifyAIChatServiceImpl.simpleChat start: sessionId {} userMessageContent {}", sessionId, userMessageContent);
		return this.openAiChatClient.prompt()
				.system(s -> s.param("current_date", LocalDate.now().toString()))
				.user(userMessageContent)
				.advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId).param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
				.call()
				.content();
	}

	@Override
	public Flux<String> streamChat(String sessionId, Long userId, String userMessageContent) {
		log.info("DifyAIChatServiceImpl.streamChat start: sessionId {} userMessageContent {}", sessionId, userMessageContent);

		// 获取或创建聊天会话
		ChatSession chatSession = chatSessionService.getSessionById(sessionId);
		if (chatSession == null) {
			chatSession = chatSessionService.createSession(userId);
		}

		// 获取聊天记录并更新大小
		SessionChatMemory chatMemory = chatSession.getChatMemory();
		log.info("DifyAIChatServiceImpl.streamChat chatMemory {}", JSON.toJSONString(chatMemory));
		// 准备消息历史
		List<Message> messages = chatSession.getChatMemory() != null
									&& !chatSession.getChatMemory().get().isEmpty() ?
											chatMemory.get() : new ArrayList<>();
		messages.add(new UserMessage(userMessageContent));

		// 创建一个 StringBuilder 用于收集流式响应
		StringBuilder assistantResponseBuilder = new StringBuilder();

		// 调用聊天模型并处理流
		// 将每个流式响应添加到 StringBuilder 中
		return this.openAiChatClient.prompt()
				.system(s -> s.param("current_date", LocalDate.now().toString()))
				.messages(messages) // 发送完整的消息历史
				.stream()
				.content()
				.doOnNext(assistantResponseBuilder::append)
				.doFinally(signalType -> {
					// 在流结束时，将完整的响应添加到消息历史中
					String assistantResponse = assistantResponseBuilder.toString();
					messages.add(new AssistantMessage(assistantResponse)); // 添加助手的消息
					chatMemory.put(messages); // 更新聊天记录
				});
	}

}
