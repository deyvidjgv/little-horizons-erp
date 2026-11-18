package com.littlehorizons.erp.model;

/** Row of the grados table: from Preescolar (level 0) to Once (level 11). */
public class Grade {

    public static final int LAST_LEVEL = 11;

    private final int id;
    private final String name;
    private final int level;

    public Grade(int id, String name, int level) {
        this.id = id;
        this.name = name;
        this.level = level;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }
}
