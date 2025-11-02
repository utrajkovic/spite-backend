package com.spite.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "exercises")
public class Exercise {
    @Id
    private String id;
    private String name;
    private String description;
    private String userId; // kome vežba pripada
    private String localVideoPath; // lokalna putanja videa na telefonu
    private String videoUrl;
    private int sets;
    private String reps;
    private int restBetweenSets;
    private int restAfterExercise;
}
