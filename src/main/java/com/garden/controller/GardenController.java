package com.garden.controller;

import com.garden.entity.CreateGardenRequest;
import com.garden.entity.Garden;
import com.garden.entity.UpdateGardenRequest;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class GardenController {

    @Inject
    EntityManager em;


    @Counted(
            value = "garden.create.calls",
    description = "Total number of createGarden() invocations")
    @Timed(
            value = "createGarden_latency",
            description = "Latency of createGarden()",
            longTask = false,
            histogram = true,
            percentiles = {0.5, 0.95, 0.99}
    )
   @Transactional
    public Garden createGarden(CreateGardenRequest request){
       Garden garden = new Garden();
       garden.setName(request.getName());
       garden.setDescription(request.getDescription());
       em.persist(garden);
       return garden;
    }

    @Counted(
            value = "garden.find.calls",
            description = "How many times findGarden() has been invoked"
    )
    @Timed(
            value = "garden.find.latency",
            description = "Time taken by findGarden()"
    )
    public Garden findGarden(UUID gardenId){
       return em.find(Garden.class, gardenId);
    }

    @Timed(
            value = "garden.list.latency",
            description = "Time taken by getAllGardens()"
    )
    public List<Garden> getAllGardens(){
       return em.createQuery("SELECT g FROM Garden g", Garden.class).getResultList();
    }

    @Counted("garden.update.calls")
    @Timed("garden.update.latency")
    @Transactional
    public Garden updateGarden(UUID gardenId, UpdateGardenRequest request){
       Garden garden = em.find(Garden.class, gardenId);
        if (garden == null){
            throw new IllegalArgumentException("Garden with id " + gardenId + " not found");
        }
        garden.setName(request.getName());
        garden.setDescription(request.getDescription());
        return garden;
    }

    @Counted("garden.delete.calls")
    @Timed("garden.delete.latency")
    @Transactional
    public void deleteGarden(UUID gardenId){
        Garden garden = em.find(Garden.class, gardenId);
        if (garden != null) {
            em.remove(garden);
        } else {
            throw new IllegalArgumentException("Garden with id " + gardenId + " not found");
        }
    }
}
