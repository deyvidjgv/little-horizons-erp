package com.littlehorizons.erp.model;

/** Row of the categorias_recurso table (Libro, Equipo de Laboratorio...). */
public class ResourceCategory {

    private final int id;
    private final String name;

    public ResourceCategory(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
