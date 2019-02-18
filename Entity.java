package au.unisa.erl.textparse.textexample.controller;

import java.util.ArrayList;

public class Entity {
    private String id;
    private String entityType;
    private String typeProbability;
    private String name;
    private ArrayList<Relations> longestRelation;
    private ArrayList<String> originalContext;


    public String getEntityType() {
        return entityType;
    }

    public String getTypeProbability() {
        return typeProbability;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ArrayList<Relations> getLongestRelation() {
        return longestRelation;
    }

    public void setLongestRelation( ArrayList<Relations> longestRelation) {
        this.longestRelation = longestRelation;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setTypeProbability(String typeProbability) {
        this.typeProbability = typeProbability;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOriginalContext(ArrayList<String> originalContext) {
        this.originalContext = originalContext;
    }

    public ArrayList<String> getOriginalContext() {
        return originalContext;
    }
}
