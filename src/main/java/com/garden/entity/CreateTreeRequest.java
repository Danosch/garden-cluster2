package com.garden.entity;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateTreeRequest {
    private String name;
    private String species;
    private Integer age;
    private Double height;
    private UUID gardenId;
}
