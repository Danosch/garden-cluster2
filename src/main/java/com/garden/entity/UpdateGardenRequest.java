package com.garden.entity;

import lombok.Data;

@Data
public class UpdateGardenRequest {
    private String name;
    private String description;
}
