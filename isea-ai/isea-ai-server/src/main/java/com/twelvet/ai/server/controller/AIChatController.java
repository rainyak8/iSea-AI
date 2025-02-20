package com.twelvet.ai.server.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.twelvet.ai.server.service.DifyAIChatService;
import com.twelvet.framework.core.application.controller.TWTController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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

	@Autowired
	private DifyAIChatService difyAgent;
	@Operation(summary = "普通对话")
	@SaIgnore
	@GetMapping(path="/simpleChat")
	public String simpleChat(String chatId, String userMessage) {
		return difyAgent.simpleChat(chatId, userMessage);
	}
	@Operation(summary = "流式对话")
	@SaIgnore
	@GetMapping(path="/streamChat")
	public Flux<String> streamChat(String chatId, String userMessage) {
		return difyAgent.streamChat(chatId, userMessage);
	}
}
