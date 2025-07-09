package com.garden.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Data
@Table(name = "trees")
public class Tree {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    public String name;

    private String species;

    private Integer age;

    private Double height;

    @Column(name = "created_at")
    private LocalDateTime created;

    @Column(name = "last_updated")
    public LocalDateTime lastUpdated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "garden_id")
    private Garden garden;

    @PrePersist
    protected void onCreate(){
        created = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
        lastUpdated = LocalDateTime.now();
    }

}
