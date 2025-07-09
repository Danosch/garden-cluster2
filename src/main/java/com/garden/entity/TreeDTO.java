package com.garden.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TreeDTO {

    private UUID id;
    private String name;
    private String species;
    private Integer age;
    private Double height;
    private LocalDateTime created;
    private LocalDateTime lastUpdated;
    private UUID gardenId;

}
