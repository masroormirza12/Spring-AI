package com.example.bot.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
public class UserData {

    private String name;
    private Date entryTime;
}
