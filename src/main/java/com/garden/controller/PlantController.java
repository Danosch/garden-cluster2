package com.garden.controller;

import com.garden.entity.CreatePlantRequest;
import com.garden.entity.Garden;
import com.garden.entity.Plant;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PlantController {

    @Inject
    EntityManager em;

    @Counted(
            value = "plant.create.calls",
            description = "How many times createPlant() has been invoked"
    )
    @Timed(
            value = "plant.create.latency",
            description = "Time taken by createPlant()",
            histogram = true,
            percentiles = {0.5, 0.95, 0.99}
    )
    @Transactional
    public Plant createPlant(CreatePlantRequest request){
        Garden garden = em.find(Garden.class, request.getGardenId());
        if (garden == null){
            throw new IllegalArgumentException("Garden with id " + request.getGardenId() + " not found");
        }

        Plant plant = new Plant();
        plant.setName(request.getName());
        plant.setSpecies(request.getSpecies());
        plant.setColor(request.getColor());
        plant.setPlantingDate(request.getPlantingDate());
        plant.setGarden(garden);
        em.persist(plant);
        return plant;
    }

    @Counted(
            value = "plant.find.calls",
            description = "How many times findPlant() has been invoked"
    )
    @Timed(
            value = "plant.find.latency",
            description = "Time taken by findPlant()"
    )
    public Plant findPlant(UUID plant){
        return em.find(Plant.class, plant);
    }

    @Counted(
            value = "plant.list.calls",
            description = "How many times getAllPlants() has been invoked"
    )
    @Timed(
            value = "plant.list.latency",
            description = "Time taken by getAllPlants()"
    )
    public List<Plant> getAllPlants(){
        return em.createQuery("SELECT p FROM Plant p", Plant.class).getResultList();
    }

    @Counted(
            value = "plant.by_garden.calls",
            description = "How many times getPlantsByGarden() has been invoked"
    )
    @Timed(
            value = "plant.by_garden.latency",
            description = "Time taken by getPlantsByGarden()"
    )
    public List<Plant> getPlantsByGarden(UUID gardenId){
        return em.createQuery("SELECT p FROM Plant p WHERE p.garden.id = :gardenId", Plant.class)
                .setParameter("gardenId", gardenId)
                .getResultList();
    }

    @Counted(
            value = "plant.delete.calls",
            description = "How many times deletePlant() has been invoked"
    )
    @Timed(
            value = "plant.delete.latency",
            description = "Time taken by deletePlant()"
    )
    @Transactional
    public void deletePlant(UUID plantId){
        Plant plant = em.find(Plant.class, plantId);
        if (plant != null) {
            em.remove(plant);
        } else {
            throw new IllegalArgumentException("Plant with id " + plantId + " not found");
        }
    }
}
