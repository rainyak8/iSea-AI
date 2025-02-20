package com.twelvet.ai.server.controller;

import com.twelvet.ai.server.service.DifyAIChatService;
import com.twelvet.framework.core.application.controller.TWTController;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @author rainyak
 * @Description: AI对话
 */
@Tag(description = "AIChatController", name = "AI对话")
@RestController
@RequestMapping("/ai/chat")
public class AIChatController extends TWTController {

	@Resource
	private DifyAIChatService agent;

	@RequestMapping(path="/simpleChat")
	public String simpleChat(String chatId, String userMessage) {
		return agent.simpleChat(chatId, userMessage);
	}

	@RequestMapping(path="/streamChat")
	public Flux<String> streamChat(String chatId, String userMessage) {
		return agent.streamChat(chatId, userMessage);
	}
}
