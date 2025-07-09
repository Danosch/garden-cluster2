package com.garden.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Data
@Entity
@Table(name = "gardens")
public class Garden {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    public String name;

    @OneToMany(mappedBy = "garden", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Tree> trees;

    @OneToMany(mappedBy = "garden", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Plant> plants;

    @Column(name = "created_at")
    private LocalDateTime created;

    @Column(name = "last_updated")
    public LocalDateTime lastUpdated;

    @PrePersist
    protected void onCreate(){
        created = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
        lastUpdated = LocalDateTime.now();
    }

    public String description;
}
