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
    private String userId;
    private String videoUrl;

    @Deprecated
    private int sets;

    @Deprecated
    private String reps;

    @Deprecated
    private int restBetweenSets;

    @Deprecated
    private int restAfterExercise;
}
