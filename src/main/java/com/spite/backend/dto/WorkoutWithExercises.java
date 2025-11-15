package com.spite.backend.dto;

import com.spite.backend.model.Exercise;

import java.util.List;

public class WorkoutWithExercises {
    private String id;
    private String title;
    private String subtitle;
    private String content;
    private List<Exercise> exercises; // puni objekti vežbi

    public WorkoutWithExercises() {}

    public WorkoutWithExercises(String id, String title, String subtitle, String content, List<Exercise> exercises) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.content = content;
        this.exercises = exercises;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getContent() { return content; }
    public List<Exercise> getExercises() { return exercises; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public void setContent(String content) { this.content = content; }
    public void setExercises(List<Exercise> exercises) { this.exercises = exercises; }
}
