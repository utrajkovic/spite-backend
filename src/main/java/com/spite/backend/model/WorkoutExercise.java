package com.spite.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutExercise {

    private String exerciseId;

    private int sets;
    private String reps;
    private int restBetweenSets; 
    private int restAfterExercise; 

    private boolean superset;   
    private String supersetWithId; 
}
