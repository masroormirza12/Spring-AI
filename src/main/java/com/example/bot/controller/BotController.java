package com.example.bot.controller;

import com.example.bot.entity.SpeechResult;
import com.example.bot.service.BotService;
import com.example.bot.service.PcmToMp3Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;


/*
This Controller is purely for testing purpose of features
 */

@RestController
@RequestMapping("/bot")
@RequiredArgsConstructor
public class BotController {

    @Autowired
    private  BotService botService;
    @Autowired
    private PcmToMp3Converter pcmToMp3Converter;


    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return botService.chatWithText(message, new ArrayList<>());
    }

    @GetMapping(value="/tts",produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> textToSpeech(@RequestParam String message) throws Exception {
        byte[] pcm = botService.textToSpeech(message);


        // Convert PCM -> MP3
        byte[] mp3 = pcmToMp3Converter.convertPcmToWav(pcm, 24000, 1);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "audio/wav")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=output.wav")
                .body(mp3);
    }

    @PostMapping(value="/sts",produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<?> speechToSpeech(@RequestBody String transcript) {
        SpeechResult result = botService.speechToSpeech(transcript);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "audio/wav")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=output.wav").body(result.getAudio());
    }



}
