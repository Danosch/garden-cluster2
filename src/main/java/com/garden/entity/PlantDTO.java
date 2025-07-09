package com.garden.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantDTO {

    private UUID id;
    private String name;
    private String species;
    private String color;
    private LocalDateTime plantingDate;
    private LocalDateTime created;
    private LocalDateTime lastUpdated;
    private UUID gardenId;
}
