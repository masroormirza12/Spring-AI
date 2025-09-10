package com.example.bot.service;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatMemoryService {

    public final Map<String, Deque<Message>> memory = new ConcurrentHashMap<>();
    private static final int MAX_MESSAGES = 15;

    //Add user messages
    public void addUserMessage(String chatId, String content) {
        addMessage(chatId, new UserMessage(content));
    }

    //Add Gemini messages
    public void addAssistantMessage(String chatId, String content) {
        addMessage(chatId, new AssistantMessage(content));
    }

    public List<Message> getHistory(String chatId) {
        return new ArrayList<>(memory.getOrDefault(chatId, new ArrayDeque<>()));
    }


    //Utility methord
    private void addMessage(String chatId, Message message) {
        memory.computeIfAbsent(chatId, k -> new ArrayDeque<>());

        Deque<Message> history = memory.get(chatId);
        if (history.size() >= MAX_MESSAGES) {
            history.pollFirst(); // remove oldest
        }
        history.addLast(message);
    }

}
