package com.garden.boundary;

import com.garden.controller.GardenController;
import com.garden.controller.PlantController;
import com.garden.controller.TreeController;
import com.garden.entity.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.List;
import io.micrometer.core.instrument.Timer;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class GardenResourceImpl implements GardenResource {

    @Inject
    GardenController gardenController;

    @Inject
    TreeController treeController;

    @Inject
    PlantController plantController;

    @Inject
    Tracer tracer;

    @Inject
    MeterRegistry registry;

    // Metriken
    private Counter createCounter;
    private Counter fetchAllCounter;
    private Timer createTimer;
    private Timer fetchAllTimer;

    @PostConstruct
    void initMetrics() {
        createCounter = registry.counter("garden_gardens_created_total");
        fetchAllCounter = registry.counter("garden_gardens_fetched_total");
        createTimer   = (Timer) registry.timer("garden_gardens_create_duration");
        fetchAllTimer = (Timer) registry.timer("garden_gardens_fetch_duration");
    }

    @Override
    public String hello() {
        Log.info("Hello from GardenResourceImpl");
        return "Welcome to our Garden Management System";
    }

    @Override
    public GardenDTO createGarden(CreateGardenRequest request) {
        // 1. Tracing-Span öffnen
        Span span = tracer.spanBuilder("GardenResource.createGarden").startSpan();
        try (var scope = span.makeCurrent()) {
            // 2. Metrik-Timer starten
            return createTimer.record(() -> {
                // 3. Business-Logik & Zähler
                createCounter.increment();
                try {
                    Garden garden = gardenController.createGarden(request);
                    Log.infof("Created garden with id %s", garden.getId());
                    span.setAttribute("garden.id", garden.getId().toString());
                    return toGardenDTO(garden);
                } catch (Exception e) {
                    span.recordException(e);
                    Log.error("Error creating garden: " + e.getMessage(), e);
                    throw new WebApplicationException(
                            "Error creating garden: " + e.getMessage(),
                            Response.Status.BAD_REQUEST
                    );
                }
            });
        } finally {
            span.end();
        }
    }

    @Override
    public List<GardenDTO> getAllGardens() {
        Span span = tracer.spanBuilder("GardenResource.getAllGardens").startSpan();
        try (var scope = span.makeCurrent()) {
            return fetchAllTimer.record(() -> {
                fetchAllCounter.increment();
                try {
                    List<Garden> gardens = gardenController.getAllGardens();
                    Log.infof("Fetched %d gardens", gardens.size());
                    span.setAttribute("gardens.count", gardens.size());
                    return gardens.stream()
                            .map(this::toGardenDTO)
                            .collect(Collectors.toList());
                } catch (Exception e) {
                    span.recordException(e);
                    Log.error("Error fetching gardens: " + e.getMessage(), e);
                    throw new WebApplicationException(
                            "Error fetching gardens: " + e.getMessage(),
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                }
            });
        } finally {
            span.end();
        }
    }

    @Override
    public GardenDTO getGarden(UUID gardenId) {
        try {
            Garden garden = gardenController.findGarden(gardenId);
            if (garden == null) {
                Log.error("Garden with id " + gardenId + " not found");
                throw new WebApplicationException("Garden not found", Response.Status.NOT_FOUND);
            }
            Log.info("Fetched garden with id " + gardenId);
            return toGardenDTO(garden);
        } catch (WebApplicationException e) {
            Log.error("Error fetching garden with id " + gardenId + ": " + e.getMessage());
            throw e;
        } catch (Exception e) {
            Log.error("Error fetching garden with id " + gardenId + ": " + e.getMessage());
            throw new WebApplicationException("Error fetching garden: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public GardenDTO updateGarden(UUID gardenId, UpdateGardenRequest request) {
        try {
            Garden garden = gardenController.updateGarden(gardenId, request);
            if (garden == null) {
                Log.error("Garden with id " + gardenId + " not found");
                throw new WebApplicationException("Garden not found", Response.Status.NOT_FOUND);
            }
            Log.info("Updated garden with id " + gardenId);
            return toGardenDTO(garden);
        } catch (WebApplicationException e) {
            Log.error("Error updating garden with id " + gardenId + ": " + e.getMessage());
            throw e;
        } catch (Exception e) {
            Log.error("Error updating garden with id " + gardenId + ": " + e.getMessage());
            throw new WebApplicationException("Error updating garden: " + e.getMessage(),
                    Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public void deleteGarden(UUID gardenId) {
        try {
            gardenController.deleteGarden(gardenId);
            Log.info("Deleted garden with id " + gardenId);
        } catch (Exception e) {
            Log.error("Error deleting garden with id " + gardenId + ": " + e.getMessage());
            throw new WebApplicationException("Error deleting garden: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public TreeDTO createTree(CreateTreeRequest request) {
        try {
            Tree tree = treeController.createTree(request);
            Log.info("Created tree with id " + tree.getId());
            return toTreeDTO(tree);
        } catch (Exception e) {
            Log.error("Error creating tree: " + e.getMessage());
            throw new WebApplicationException("Error creating tree: " + e.getMessage(),
                    Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public List<TreeDTO> getAllTrees() {
        try {
            List<Tree> trees = treeController.getAllTrees();
            Log.info("Fetched " + trees.size() + " trees");
            return trees.stream()
                    .map(this::toTreeDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            Log.error("Error fetching trees: " + e.getMessage());
            throw new WebApplicationException("Error fetching trees: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<TreeDTO> getTreesByGarden(UUID gardenId) {
        try {
            List<Tree> trees = treeController.getTreesByGarden(gardenId);
            Log.info("Fetched " + trees.size() + " trees");
            return trees.stream()
                    .map(this::toTreeDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            Log.error("Error fetching trees: " + e.getMessage());
            throw new WebApplicationException("Error fetching trees: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void deleteTree(UUID treeId) {
        try {
            Log.info("Deleting tree with id " + treeId);
            treeController.deleteTree(treeId);
        } catch (Exception e) {
            Log.error("Error deleting tree with id " + treeId + ": " + e.getMessage());
            throw new WebApplicationException("Error deleting tree: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public PlantDTO createPlant(CreatePlantRequest request) {
        try {
            Plant plant = plantController.createPlant(request);
            Log.info("Created plant with id " + plant.getId());
            return toPlantDTO(plant);
        } catch (Exception e) {
            Log.error("Error creating plant: " + e.getMessage());
            throw new WebApplicationException("Error creating plant: " + e.getMessage(),
                    Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public List<PlantDTO> getAllPlants() {
        try {
            List<Plant> plants = plantController.getAllPlants();
            Log.info("Fetched " + plants.size() + " plants");
            return plants.stream()
                    .map(this::toPlantDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            Log.error("Error fetching plants: " + e.getMessage());
            throw new WebApplicationException("Error fetching plants: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<PlantDTO> getPlantsByGarden(UUID gardenId) {
        try {
            List<Plant> plants = plantController.getPlantsByGarden(gardenId);
            Log.info("Fetched " + plants.size() + " plants");
            return plants.stream()
                    .map(this::toPlantDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            Log.error("Error fetching plants: " + e.getMessage());
            throw new WebApplicationException("Error fetching plants: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void deletePlant(UUID plantId) {
        try {
            Log.info("Deleting plant with id " + plantId);
            plantController.deletePlant(plantId);
        } catch (Exception e) {
            Log.error("Error deleting plant with id " + plantId + ": " + e.getMessage());
            throw new WebApplicationException("Error deleting plant: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private GardenDTO toGardenDTO(Garden garden) {
        List<TreeDTO> treeDTOs = garden.getTrees() != null ?
                garden.getTrees().stream().map(this::toTreeDTO).collect(Collectors.toList()) : null;
        List<PlantDTO> plantDTOs = garden.getPlants() != null ?
                garden.getPlants().stream().map(this::toPlantDTO).collect(Collectors.toList()) : null;

        return new GardenDTO(
                garden.getId(),
                garden.getName(),
                garden.getDescription(),
                garden.getCreated(),
                garden.getLastUpdated(),
                treeDTOs,
                plantDTOs
        );
    }

    private TreeDTO toTreeDTO(Tree tree) {
        return new TreeDTO(
                tree.getId(),
                tree.getName(),
                tree.getSpecies(),
                tree.getAge(),
                tree.getHeight(),
                tree.getCreated(),
                tree.getLastUpdated(),
                tree.getGarden() != null ? tree.getGarden().getId() : null
        );
    }

    private PlantDTO toPlantDTO(Plant plant) {
        return new PlantDTO(
                plant.getId(),
                plant.getName(),
                plant.getSpecies(),
                plant.getColor(),
                plant.getPlantingDate(),
                plant.getCreated(),
                plant.getLastUpdated(),
                plant.getGarden() != null ? plant.getGarden().getId() : null
        );
    }

}
