package com.spite.backend.model;

public class ExerciseFeedback {

    private String exerciseId;
    private String exerciseName;
    private int sets;
    private String reps;
    private int doneSets;
    private int doneReps;
    private Double maxKg;
    private String intensity; // easy | normal | hard

    public ExerciseFeedback() {}

    public String getExerciseId() { return exerciseId; }
    public void setExerciseId(String exerciseId) { this.exerciseId = exerciseId; }

    public String getExerciseName() { return exerciseName; }
    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }

    public int getSets() { return sets; }
    public void setSets(int sets) { this.sets = sets; }

    public String getReps() { return reps; }
    public void setReps(String reps) { this.reps = reps; }

    public int getDoneSets() { return doneSets; }
    public void setDoneSets(int doneSets) { this.doneSets = doneSets; }

    public int getDoneReps() { return doneReps; }
    public void setDoneReps(int doneReps) { this.doneReps = doneReps; }

    public Double getMaxKg() { return maxKg; }
    public void setMaxKg(Double maxKg) { this.maxKg = maxKg; }

    public String getIntensity() { return intensity; }
    public void setIntensity(String intensity) { this.intensity = intensity; }
}
