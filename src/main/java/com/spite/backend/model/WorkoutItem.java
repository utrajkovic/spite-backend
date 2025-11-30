package com.spite.backend.model;

public class WorkoutItem {

    private String exerciseId;
    private int sets;
    private String reps;
    private int restBetweenSets;
    private int restAfterExercise;
    private boolean superset;

    public WorkoutItem() {
    }

    public WorkoutItem(String exerciseId,
            int sets,
            String reps,
            int restBetweenSets,
            int restAfterExercise,
            boolean superset) {
        this.exerciseId = exerciseId;
        this.sets = sets;
        this.reps = reps;
        this.restBetweenSets = restBetweenSets;
        this.restAfterExercise = restAfterExercise;
        this.superset = superset;
    }

    public String getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(String exerciseId) {
        this.exerciseId = exerciseId;
    }

    public int getSets() {
        return sets;
    }

    public void setSets(int sets) {
        this.sets = sets;
    }

    public String getReps() {
        return reps;
    }

    public void setReps(String reps) {
        this.reps = reps;
    }

    public int getRestBetweenSets() {
        return restBetweenSets;
    }

    public void setRestBetweenSets(int restBetweenSets) {
        this.restBetweenSets = restBetweenSets;
    }

    public int getRestAfterExercise() {
        return restAfterExercise;
    }

    public void setRestAfterExercise(int restAfterExercise) {
        this.restAfterExercise = restAfterExercise;
    }

    public boolean isSuperset() {
        return superset;
    }

    public void setSuperset(boolean superset) {
        this.superset = superset;
    }
}
