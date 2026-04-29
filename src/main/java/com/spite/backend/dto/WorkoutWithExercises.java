package com.spite.backend.dto;

import com.spite.backend.model.Exercise;
import com.spite.backend.model.WorkoutItem;

import java.util.List;

public class WorkoutWithExercises {
    private String id;
    private String title;
    private String subtitle;
    private String content;
    private String note;
    private List<Exercise> exercises;
    private List<WorkoutItem> items;

    public WorkoutWithExercises() {}

    public WorkoutWithExercises(String id, String title, String subtitle, String content, List<Exercise> exercises) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.content = content;
        this.exercises = exercises;
    }

    public WorkoutWithExercises(String id, String title, String subtitle, String content, String note, List<Exercise> exercises, List<WorkoutItem> items) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.content = content;
        this.note = note;
        this.exercises = exercises;
        this.items = items;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getContent() { return content; }
    public String getNote() { return note; }
    public List<Exercise> getExercises() { return exercises; }
    public List<WorkoutItem> getItems() { return items; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public void setContent(String content) { this.content = content; }
    public void setNote(String note) { this.note = note; }
    public void setExercises(List<Exercise> exercises) { this.exercises = exercises; }
    public void setItems(List<WorkoutItem> items) { this.items = items; }
}
