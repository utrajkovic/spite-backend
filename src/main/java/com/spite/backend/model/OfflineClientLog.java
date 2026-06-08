package com.spite.backend.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Jedan zapis treninga za offline klijenta: datum + šta je rađeno,
 * opciono vežbe, rekordi i telesna kilaža (za praćenje napretka).
 */
@Document(collection = "offline_client_logs")
public class OfflineClientLog {

    @Id
    private String id;

    @Indexed
    private String offlineClientId;

    private String trainerUsername;
    private long date;            // epoch millis (dan treninga)
    private String workoutTitle;  // šta je rađeno
    private String note;
    private Double bodyWeightKg;   // opciono — za grafikon napretka
    private List<Exercise> exercises = new ArrayList<>();
    private List<Record> records = new ArrayList<>();
    private long createdAt;

    public OfflineClientLog() {}

    public static class Exercise {
        private String name;
        private Integer sets;
        private String reps;
        private Double weightKg;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Integer getSets() { return sets; }
        public void setSets(Integer sets) { this.sets = sets; }

        public String getReps() { return reps; }
        public void setReps(String reps) { this.reps = reps; }

        public Double getWeightKg() { return weightKg; }
        public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }
    }

    public static class Record {
        private String name;
        private Double value;
        private String unit;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Double getValue() { return value; }
        public void setValue(Double value) { this.value = value; }

        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOfflineClientId() { return offlineClientId; }
    public void setOfflineClientId(String offlineClientId) { this.offlineClientId = offlineClientId; }

    public String getTrainerUsername() { return trainerUsername; }
    public void setTrainerUsername(String trainerUsername) { this.trainerUsername = trainerUsername; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }

    public String getWorkoutTitle() { return workoutTitle; }
    public void setWorkoutTitle(String workoutTitle) { this.workoutTitle = workoutTitle; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Double getBodyWeightKg() { return bodyWeightKg; }
    public void setBodyWeightKg(Double bodyWeightKg) { this.bodyWeightKg = bodyWeightKg; }

    public List<Exercise> getExercises() { return exercises; }
    public void setExercises(List<Exercise> exercises) { this.exercises = exercises; }

    public List<Record> getRecords() { return records; }
    public void setRecords(List<Record> records) { this.records = records; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
