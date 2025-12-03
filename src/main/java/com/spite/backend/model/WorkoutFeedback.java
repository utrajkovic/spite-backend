package com.spite.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document("workout_feedback")
public class WorkoutFeedback {

    @Id
    private String id;

    private String userId;
    private String workoutId;
    private long timestamp;

    private List<ExerciseFeedback> exercises;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getWorkoutId() {
        return workoutId;
    }

    public void setWorkoutId(String workoutId) {
        this.workoutId = workoutId;
    }

    public List<ExerciseFeedback> getExercises() {
        return exercises;
    }

    public void setExercises(List<ExerciseFeedback> exercises) {
        this.exercises = exercises;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public static class ExerciseFeedback {
        private String exerciseId;
        private String exerciseName;

        private int sets;
        private String reps;

        private int doneSets;
        private int doneReps;

        private Integer maxKg;
        private String intensity;

        // getter/setteri i ovde
        public String getExerciseId() {
            return exerciseId;
        }

        public void setExerciseId(String exerciseId) {
            this.exerciseId = exerciseId;
        }

        public String getExerciseName() {
            return exerciseName;
        }

        public void setExerciseName(String exerciseName) {
            this.exerciseName = exerciseName;
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

        public int getDoneSets() {
            return doneSets;
        }

        public void setDoneSets(int doneSets) {
            this.doneSets = doneSets;
        }

        public int getDoneReps() {
            return doneReps;
        }

        public void setDoneReps(int doneReps) {
            this.doneReps = doneReps;
        }

        public Integer getMaxKg() {
            return maxKg;
        }

        public void setMaxKg(Integer maxKg) {
            this.maxKg = maxKg;
        }

        public String getIntensity() {
            return intensity;
        }

        public void setIntensity(String intensity) {
            this.intensity = intensity;
        }
    }
}
