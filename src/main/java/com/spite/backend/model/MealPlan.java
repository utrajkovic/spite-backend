package com.spite.backend.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("meal_plans")
public class MealPlan {

    @Id
    private String id;

    @Indexed(unique = true)
    private String clientUsername;

    private String trainerUsername;
    private List<Meal> meals = new ArrayList<>();
    private long updatedAt;

    public MealPlan() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClientUsername() { return clientUsername; }
    public void setClientUsername(String clientUsername) { this.clientUsername = clientUsername; }

    public String getTrainerUsername() { return trainerUsername; }
    public void setTrainerUsername(String trainerUsername) { this.trainerUsername = trainerUsername; }

    public List<Meal> getMeals() { return meals; }
    public void setMeals(List<Meal> meals) { this.meals = meals; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public static class Meal {
        private String name;
        private String foods;

        public Meal() {}

        public Meal(String name, String foods) {
            this.name = name;
            this.foods = foods;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getFoods() { return foods; }
        public void setFoods(String foods) { this.foods = foods; }
    }
}
