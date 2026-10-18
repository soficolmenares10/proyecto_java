package com.littlehorizons.erp.model;

/**
 * A school grade (Preescolar, Primero, ... Once).
 */
public class Grade {

    private int id;
    private String name;
    private int numericLevel;

    public Grade() {
    }

    public Grade(int id, String name, int numericLevel) {
        this.id = id;
        this.name = name;
        this.numericLevel = numericLevel;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumericLevel() {
        return numericLevel;
    }

    public void setNumericLevel(int numericLevel) {
        this.numericLevel = numericLevel;
    }

    @Override
    public String toString() {
        return name;
    }
}
