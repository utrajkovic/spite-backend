package com.spite.backend.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Jedna stavka korisničkog (CLIENT) podsetnika za trening.
 * Npr. dani = [1,3,5] (Pon/Sre/Pet), time = "07:00", note = "Gornji deo tela".
 */
public class WorkoutReminder {

    // ISO dani u nedelji: 1=Ponedeljak ... 7=Nedelja
    private List<Integer> days = new ArrayList<>();
    private String time = "07:00"; // HH:mm
    private String note = "";

    public WorkoutReminder() {}

    public List<Integer> getDays() { return days; }
    public void setDays(List<Integer> days) { this.days = days; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
