package com.garden.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;
@Data
public class CreatePlantRequest {

    private String name;
    private String species;
    private String color;
    private LocalDateTime plantingDate;
    private UUID gardenId;
}
