package com.twelvet.ai.server.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;


/**
 * @author rainyak
 * @Description: Dify AI对话
 */
public interface DifyAIChatService {
	/**
	 * 简单调用
	 */
	String simpleChat(String chatId, String userMessageContent);

	/**
	 * 流式调用
	 */
	Flux<String> streamChat(String chatId, String userMessageContent);
}
