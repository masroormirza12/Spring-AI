package com.example.bot.controller;


import com.example.bot.entity.UserData;
import com.example.bot.service.ChatMemoryService;
import com.example.bot.service.GeminiSpeechService;
import com.example.bot.service.PcmToMp3Converter;
import com.example.bot.service.TelegramWebHookService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/*
This the Main controller/entry point for telegram webhook via you server
 */
@RestController
@RequestMapping("/Telegram")
@RequiredArgsConstructor
public class TelegramWebHook {


    @Autowired
    private TelegramWebHookService telegramWebHookService;
    @Lazy
    @Autowired
    private GeminiSpeechService geminiSpeechService;
    @Lazy
    @Autowired
    private PcmToMp3Converter pcmToMp3Converter;
    @Lazy
    @Autowired
    private  ChatMemoryService chatMemoryService;

    public static Map<String, UserData> userMap = new ConcurrentHashMap<>();
    private static final long EXPIRY_MS = 50_000;

    @PostMapping("/webhook")
    public ResponseEntity<String> onUpdate(@RequestBody Map<String, Object> update) {
        System.out.println("Raw update: " + update);

        Map<String, Object> message =telegramWebHookService.safeCast(update.get("message"));
        if (message == null) {
            return ResponseEntity.badRequest().body("Message is null");
        }

        Map<String, Object> chat = telegramWebHookService.safeCast(message.get("chat"));
        if (chat == null || !chat.containsKey("id")) {
            return ResponseEntity.badRequest().body("Chat info missing");
        }

        String chatId = chat.get("id").toString();
        String name = String.valueOf(chat.getOrDefault("first_name", "Unknown"));

        // Run async to avoid blocking Telegram webhook
        CompletableFuture.runAsync(() -> telegramWebHookService.handleMessage(message, chatId, name));

        //This is purely to keep track of users
        Date now = new Date();
        userMap.compute(chatId, (key, existing) -> {
            if (existing == null || (now.getTime() - existing.getEntryTime().getTime()) > EXPIRY_MS) {
                return new UserData(name, now); // replace with new session
            }
            return existing; // keep old one if still valid
        });

        return ResponseEntity.accepted().body("Got the input");
    }


    @GetMapping("/getUsers")
    public ResponseEntity<?> getUsers() {
        return ResponseEntity.ok().body(userMap);
    }

    @GetMapping("/getUsersMessages")
    public ResponseEntity<?> getUsers(@RequestParam(required = true) String chatId) {
        return ResponseEntity.ok().body(chatMemoryService.getHistory(chatId));
    }


}
