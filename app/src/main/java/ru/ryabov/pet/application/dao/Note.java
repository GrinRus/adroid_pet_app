package ru.ryabov.pet.application.dao;

import lombok.Data;

@Data
public class Note {
    private String dateTime;
    private String text;
    private String userId;
}
