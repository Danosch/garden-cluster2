package com.garden.entity;

import lombok.Data;

@Data
public class CreateGardenRequest {
    private String name;
    private String description;
}
