package com.spite.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "workouts")
public class Workout {

    @Id
    private String id;
    private String title;
    private String subtitle;
    private String content;
    private List<String> exerciseIds;

    public Workout() {}

    public Workout(String id, String title, String subtitle, String content, List<String> exerciseIds) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.content = content;
        this.exerciseIds = exerciseIds;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<String> getExerciseIds() { return exerciseIds; }
    public void setExerciseIds(List<String> exerciseIds) { this.exerciseIds = exerciseIds; }
}
