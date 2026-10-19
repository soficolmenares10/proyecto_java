package com.littlehorizons.erp.model;

import java.time.LocalDate;
import java.util.List;

/**
 * A school event (Feria de Ciencias, Día Cultural, ...). Maps the table "eventos".
 * It is called SchoolEvent to avoid confusion with Java's own event classes.
 */
public class SchoolEvent {

    public static final List<String> TYPES = List.of("Académico", "Cultural", "Deportivo", "Social", "Científico");

    private int id;
    private String name;
    private String description;
    private String type;
    private LocalDate date;
    private String place;
    private boolean competition;
    private Integer organizerId;
    private String organizerName;
    private boolean active;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public boolean isCompetition() {
        return competition;
    }

    public void setCompetition(boolean competition) {
        this.competition = competition;
    }

    public Integer getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(Integer organizerId) {
        this.organizerId = organizerId;
    }

    public String getOrganizerName() {
        return organizerName;
    }

    public void setOrganizerName(String organizerName) {
        this.organizerName = organizerName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
