package com.garden.boundary;

import com.garden.entity.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.UUID;

@Path("/garden")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface GardenResource {

    @GET
    String hello();

    @POST
    @Path("/create")
    GardenDTO createGarden(CreateGardenRequest request);

    @GET
    @Path("/all")
    List<GardenDTO> getAllGardens();

    @GET
    @Path("/{gardenId}")
    GardenDTO getGarden(@PathParam("gardenId") UUID gardenId);

    @PUT
    @Path("/{gardenId}")
    GardenDTO updateGarden(@PathParam("gardenId") UUID gardenId, UpdateGardenRequest request);

    @DELETE
    @Path("/{gardenId}")
    void deleteGarden(@PathParam("gardenId") UUID gardenId);

    @POST
    @Path("/tree/create")
    TreeDTO createTree(CreateTreeRequest request);

    @GET
    @Path("/tree/all")
    List<TreeDTO> getAllTrees();

    @GET
    @Path("/{gardenId}/trees")
    List<TreeDTO> getTreesByGarden(@PathParam("gardenId") UUID gardenId);

    @DELETE
    @Path("/tree/{treeId}")
    void deleteTree(@PathParam("treeId") UUID treeId);

    @POST
    @Path("/plant/create")
    PlantDTO createPlant(CreatePlantRequest request);

    @GET
    @Path("/plant/all")
    List<PlantDTO> getAllPlants();

    @GET
    @Path("/{gardenId}/plants")
    List<PlantDTO> getPlantsByGarden(@PathParam("gardenId") UUID gardenId);

    @DELETE
    @Path("/plant/{plantId}")
    void deletePlant(@PathParam("plantId") UUID plantId);

}
