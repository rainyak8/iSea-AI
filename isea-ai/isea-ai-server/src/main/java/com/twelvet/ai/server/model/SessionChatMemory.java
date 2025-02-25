package com.twelvet.ai.server.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class SessionChatMemory {

	List<Message> conversationHistory;
	public SessionChatMemory() {
		this.conversationHistory = new ArrayList<>();
	}

	public void add(Message message) {
		conversationHistory.add(message);
	}

	public void add(List<Message> messages) {
		conversationHistory.addAll(messages);
	}

	public List<Message> get() {
		List<Message> all = this.conversationHistory;
		return all != null ? all : List.of();
	}

	public void clear(String conversationId) {
		this.conversationHistory = new ArrayList<>();
	}

	public void put(List<Message> messages) {
		this.conversationHistory = messages;
	}
}