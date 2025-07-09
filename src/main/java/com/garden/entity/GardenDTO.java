package com.garden.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GardenDTO {

    private UUID id;
    private String name;
    private String description;
    private LocalDateTime created;
    private LocalDateTime lastUpdated;
    private List<TreeDTO> trees;
    private List<PlantDTO> plants;
}
