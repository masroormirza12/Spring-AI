package com.example.bot.service;


import com.example.bot.entity.UserData;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.google.common.base.Strings;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Data
@Service
public class TelegramWebHookService {

    @Value("${TELEGRAM_URL}")
    private String TelegramURL;
    @Value("${TELEGRAM_TOKEN}")
    private String TelegramToken;
    @Value("${TELEGRAM_FILE_URL}")
    private String TelegramFileURL;

    private static final Logger log = LoggerFactory.getLogger(TelegramWebHookService.class);

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private BotService botService;
    @Lazy
    @Autowired
    private GeminiSpeechService geminiSpeechService;
    @Lazy
    @Autowired
    private PcmToMp3Converter pcmToMp3Converter;
    @Autowired
    private ChatMemoryService chatMemoryService;



    public Map<String, Object> safeCast(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return null;
    }


    public void handleMessage(Map<String, Object> message, String chatId, String name) {
        try {
            if (message.containsKey("text")) {
                handleTextMessage(message, chatId, name);
            } else if (message.containsKey("voice")) {
                handleVoiceMessage(message, chatId, name);
            } else {
                System.out.println("Unsupported message type from chatId " + chatId);
            }
        } catch (Exception e) {
            System.out.println("Error handling message for chatId " + chatId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleVoiceMessage(Map<String, Object> message, String chatId, String name) throws Exception {
        Map<String, Object> voice = safeCast(message.get("voice"));
        if (voice == null || !voice.containsKey("file_id")) {
            throw new IllegalArgumentException("Voice file data missing");
        }
        String fileId = voice.get("file_id").toString();
        System.out.println("Received voice message fileId: " + fileId + " chatId: " + chatId);

        // 1. Download voice from Telegram
        byte[] bytes = downloadFile(fileId);
        // 2. Upload file to Gemini
        String fileUri = geminiSpeechService.uploadAudio(bytes, "audio/ogg", "telegram-voice");
        // 3. Transcribe with Gemini
        String transcription = geminiSpeechService.transcribeAudio(fileUri, "audio/ogg");
        // 4. Send back as voice response
        sendVoice(chatId, transcription, name);
    }


    private void handleTextMessage(Map<String, Object> message, String chatId, String name) {
        String text = message.get("text").toString();
       log.info(MessageFormat.format("User {0} said {1}",name,text));
       getChatResponseFromLLM(chatId, name, text);
    }


    public void getChatResponseFromLLM(String chatId,String name, String text) {
        //Get past messages For user
        List<Message> history = chatMemoryService.getHistory(chatId);
        String LLMReply = botService.chatWithText(text+name ,history);
        // Prepare reply
       if(!Strings.isNullOrEmpty(LLMReply)) {
           Map<String, Object>body = new HashMap<>();
           body.put("chat_id", chatId);
           body.put("text", LLMReply);
           try{
               HttpHeaders headers = new HttpHeaders();
               headers.setContentType(MediaType.APPLICATION_JSON);
               HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
               ResponseEntity<String> exchange = restTemplate.exchange(getTelegramURL() + getTelegramToken() + "/sendMessage", HttpMethod.POST, request, String.class);
           }catch (Exception e){
               log.info("restCall not working" +e.getMessage());
           }
       }else {
           log.info("LLM Not responding");
       }
        CompletableFuture.runAsync(() -> {
            saveUserAndLlmMessages(chatId, text + name, LLMReply);
        });
    }

    //This is purely for saving chatMemory
    private void saveUserAndLlmMessages(String chatId, String userInput, String llmReply) {
        // Save user input
        chatMemoryService.addUserMessage(chatId, userInput);
        // Save bot reply
        chatMemoryService.addAssistantMessage(chatId,llmReply);
    }

    //Downloading file from Telegram
    public byte[] downloadFile(String fileId) {

        // Step 1: Get Telegram file metadata
        String filePathUrl = getTelegramURL() + getTelegramToken() + "/getFile?file_id=" + fileId;

        Map<String, Object> response = restTemplate.getForObject(filePathUrl, Map.class);
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        String filePath = (String) result.get("file_path");

        log.info("Telegram file path: {}", filePath);
        // Step 2: Build the actual file download URL
        String downloadUrl = getTelegramFileURL() + getTelegramToken() + "/" + filePath;
        // Step 3: Download the file as byte[]
        return restTemplate.getForObject(downloadUrl, byte[].class);
    }


    //Responding Back to Telegram
    public void sendVoice(String chatId, String trans,String name) {

        byte[] audioBytes = new byte[0];
        String LLMReply="";
        log.info("Getting reply from LLM and conveting into speech");
        try {
            List<Message> history = chatMemoryService.getHistory(chatId);
            LLMReply = botService.chatWithText(trans + name, history);
            byte[] pcm = geminiSpeechService.textToSpeech(LLMReply);
            // Convert PCM -> MP3
            audioBytes= pcmToMp3Converter.convertPcmToWav(pcm, 24000, 1);

        log.info("sending voice now to telegram");
        String url = getTelegramURL()+getTelegramToken()+"/sendVoice";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("chat_id", chatId);
        body.add("text",name);
        body.add("voice", new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return "reply.ogg";
            }
        });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            restTemplate.postForObject(url, requestEntity, String.class);
        }catch (Exception e){
            log.info("eroor hai bahi file nahi jariiii");
        }
        String finalLLMReply = LLMReply;
        CompletableFuture.runAsync(() -> {
            saveUserAndLlmMessages(chatId, trans + name, finalLLMReply);
        });
    }

}
