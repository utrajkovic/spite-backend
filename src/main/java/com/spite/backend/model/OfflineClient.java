package com.spite.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Klijent uživo koji NIJE korisnik aplikacije. Trener ga vodi ručno
 * (profil + dnevnik treninga). Vlasnik je uvek trener (trainerUsername).
 */
@Document(collection = "offline_clients")
public class OfflineClient {

    @Id
    private String id;

    @Indexed
    private String trainerUsername;

    private String name;
    private String email;
    private Double heightCm;
    private Double weightKg; // trenutna kilaža
    private String goal;
    private String notes;
    private long createdAt;

    public OfflineClient() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTrainerUsername() { return trainerUsername; }
    public void setTrainerUsername(String trainerUsername) { this.trainerUsername = trainerUsername; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Double getHeightCm() { return heightCm; }
    public void setHeightCm(Double heightCm) { this.heightCm = heightCm; }

    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
