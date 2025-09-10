package com.example.bot.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SpeechResult {
    String text;
    byte[] audio;

}
