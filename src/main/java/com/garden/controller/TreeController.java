package com.garden.controller;

import com.garden.entity.CreateTreeRequest;
import com.garden.entity.Garden;
import com.garden.entity.Tree;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TreeController {

    @Inject
    EntityManager em;

    @Counted(
            value = "tree.create.calls",
            description = "How many times createTree() has been invoked"
    )
    @Timed(
            value = "tree.create.latency",
            description = "Time taken by createTree()",
            histogram = true,
            percentiles = {0.5, 0.95, 0.99}
    )
    @Transactional
    public Tree createTree(CreateTreeRequest request){
        Garden garden = em.find(Garden.class, request.getGardenId());
        if(garden == null){
            throw new IllegalArgumentException("Garden with id " + request.getGardenId() + " not found");
        }
        Tree tree = new Tree();
        tree.setName(request.getName());
        tree.setSpecies(request.getSpecies());
        tree.setAge(request.getAge());
        tree.setHeight(request.getHeight());
        tree.setGarden(garden);
        em.persist(tree);
        return tree;
    }

    @Counted(
            value = "tree.find.calls",
            description = "How many times findTree() has been invoked"
    )
    @Timed(
            value = "tree.find.latency",
            description = "Time taken by findTree()"
    )
    public Tree findTree(UUID treeId){
        return em.find(Tree.class, treeId);
    }

    @Counted(
            value = "tree.list.calls",
            description = "How many times getAllTrees() has been invoked"
    )
    @Timed(
            value = "tree.list.latency",
            description = "Time taken by getAllTrees()"
    )
    public List<Tree> getAllTrees(){
        return em.createQuery("SELECT t FROM Tree t", Tree.class).getResultList();
    }

    @Counted(
            value = "tree.by_garden.calls",
            description = "How many times getTreesByGarden() has been invoked"
    )
    @Timed(
            value = "tree.by_garden.latency",
            description = "Time taken by getTreesByGarden()"
    )
    public List<Tree> getTreesByGarden(UUID gardenId){
        return em.createQuery("SELECT t FROM Tree t WHERE t.garden.id = :gardenId", Tree.class)
                .setParameter("gardenId", gardenId)
                .getResultList();
    }

    @Counted(
            value = "tree.delete.calls",
            description = "How many times deleteTree() has been invoked"
    )
    @Timed(
            value = "tree.delete.latency",
            description = "Time taken by deleteTree()"
    )
    @Transactional
    public void deleteTree(UUID treeId){
        Tree tree = em.find(Tree.class, treeId);
        if (tree != null) {
            em.remove(tree);
        } else {
            throw new IllegalArgumentException("Tree with id " + treeId + " not found");
        }
    }

}
