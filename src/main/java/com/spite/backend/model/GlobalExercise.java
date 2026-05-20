package com.spite.backend.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "global_exercises")
public class GlobalExercise {
    @Id
    private String id;
    private String name;
    private String description;
    private List<String> muscleGroups; // e.g. ["chest", "triceps"]
    private String videoUrl;
    private int defaultSets;
    private String defaultReps;
    private int defaultRestBetweenSets;
    private int defaultRestAfterExercise;
    private String category; // "compound" or "isolation"
    private int sortOrder; // compound prvo pa isolation
}