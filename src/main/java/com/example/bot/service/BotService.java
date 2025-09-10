package com.example.bot.service;

import com.example.bot.entity.SpeechResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class BotService {

    @Autowired
    private  ChatClient chatClient;

    @Autowired
    private GeminiSpeechService geminiSpeechService;

    @Autowired
    private PcmToMp3Converter pcmToMp3Converter;

    @Autowired
    private ChatMemoryService chatMemoryService;


    public String chatWithText(String userInput, List<Message> history) {
        return chatClient.prompt().messages(history).user(userInput).call().content();
    }

    //For Testing
    public byte[] textToSpeech(String text) {
       return  geminiSpeechService.textToSpeech(text) ;
    }

    //For Testing
    public SpeechResult speechToSpeech(String transcript) {
        String replyText = chatWithText(transcript, new ArrayList<>());
        byte[] replyAudio=null;
        try {
           replyAudio = textToSpeech(replyText);
           replyAudio= pcmToMp3Converter.convertPcmToWav(replyAudio, 24000, 1);
        } catch (Exception e) {
           log.info("audio not got ");
        }
        return new SpeechResult(replyText, replyAudio);
    }


}
