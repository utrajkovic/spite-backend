package com.spite.backend.config;

import com.spite.backend.model.Exercise;
import com.spite.backend.model.Workout;
import com.spite.backend.repository.ExerciseRepository;
import com.spite.backend.repository.WorkoutRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private final ExerciseRepository exerciseRepository;
    private final WorkoutRepository workoutRepository;

    private static final String VIDEO_BASE_PATH = "src/main/resources/uploads/videos/";

    public DataInitializer(ExerciseRepository exerciseRepository, WorkoutRepository workoutRepository) {
        this.exerciseRepository = exerciseRepository;
        this.workoutRepository = workoutRepository;
    }

    @Override
    public void run(String... args) {
        if (exerciseRepository.count() == 0) {

            List<Exercise> exercises = Arrays.asList(
                new Exercise(null, "Push-Ups", "Klasični sklekovi za grudi i triceps.", "/videos/Push-Ups.mp4", 3, "10", 60, 180),
                new Exercise(null, "Pull-Ups", "Podizi telo rukama dok visis", "/videos/Pull-Ups.mp4", 3, "8", 90, 180),
                new Exercise(null, "Squats", "Čučnjevi za noge i gluteus.", "/videos/Squats.mp4", 3, "12", 60, 180),
                new Exercise(null, "Lunges", "Iskoraci za noge i ravnotežu.", null, 3, "12", 60, 180),
                new Exercise(null, "Warm up", "Radi kombinacije sklekova, skokova, čučnjeva u zavisnosti koji deo tela radis", "/videos/Plyo-Push-Up.mp4", 3, "10", 30, 90),
                new Exercise(null, "Plyo Push-Ups", "Odguraj telo i zameni ruku na platformi", "/videos/Plyo-Push-Up.mp4", 3, "10", 60, 180),
                new Exercise(null, "Dumbell Row", "Iz skleka veslaj jednom bucicom dok druga ruka drži ravnotežu", "/videos/Dumbel-Row.mp4", 3, "10", 60, 180),
                new Exercise(null, "Lunge dumbell rise", "Iskorak sa podizanjem bucice iznad glave", "/videos/Lunge-Dumbel-Rise.mp4", 3, "10", 60, 180),
                new Exercise(null, "Chest press hip thrust", "Ležeći na podu, podiži kukove uz potisak bucicama", "/videos/Chest-Press-Hip-Thrust.mp4", 3, "10", 60, 180),
                new Exercise(null, "One leg standing row", "Veslanje bucicom dok stojiš na jednoj nozi", "/videos/One-Leg-Standing-Row.mp4", 3, "10", 60, 180),
                new Exercise(null, "One leg standing shoulder press", "Jednoručni potisak dok stojiš na jednoj nozi", "/videos/One-Leg-Shoulder-Press.mp4", 3, "10", 60, 180),
                new Exercise(null, "Unilateral floor chest press", "Potisak jednom rukom dok druga noga podiže telo", "/videos/Unilateral-Floor-Chest-Press.mp4", 3, "10", 60, 180),
                new Exercise(null, "Core rotations", "Rotacije trupa sa tegom", "/videos/Core-Rotations.mp4", 3, "12", 60, 180),
                new Exercise(null, "Barbell Squats", "Čučanj sa šipkom", "/videos/Squats.mp4", 3, "8", 60, 180),
                new Exercise(null, "Barbell Reverse Lunge", "Iskorak unazad sa šipkom", "/videos/Barbell-Reverse-Lunges.mp4", 3, "8", 60, 180),
                new Exercise(null, "Landmine Rotation", "Rotacija tela sa šipkom", "/videos/Landmine-Rotation.mp4", 3, "6", 60, 180),
                new Exercise(null, "High Weighted Skipp", "Visoki skip sa opterećenjem", "/videos/High-Weighted-Skipp.mp4", 3, "20s", 60, 180),
                new Exercise(null, "Landmine Lateral Lunges", "Bočni iskorak sa šipkom", "/videos/Landmine-Lateral-Lunges.mp4", 3, "6", 60, 180),
                new Exercise(null, "Landmine Explosive Drill", "Eksplozivni jednorucni izbacaj sa promenom", "/videos/Landmine-Explosive-Drill.mp4", 3, "12", 60, 180),
                new Exercise(null, "Landmine Shoulder Press", "Jednoručni potisak ramena", "/videos/Landmine-Shoulder-Press.mp4", 3, "8", 60, 180)
            );

            exercises.forEach(ex -> {
                if (ex.getVideoUrl() != null) {
                    File f = new File(VIDEO_BASE_PATH + new File(ex.getVideoUrl()).getName());
                    if (!f.exists()) {
                        System.out.println("⚠️ Video not found for: " + ex.getName() + " -> " + f.getPath());
                    }
                }
            });

            exercises = exerciseRepository.saveAll(exercises);
            System.out.println("✅ Dodato " + exercises.size() + " vežbi u bazu (sa backend video URL-ovima).");

            Map<String, String> ids = exercises.stream()
                    .collect(Collectors.toMap(Exercise::getName, Exercise::getId));

            if (workoutRepository.count() == 0) {
                List<Workout> workouts = Arrays.asList(
                    new Workout(null, "Upper Body 1", "Strength, Plyo", "Fokus na grudi, ramena, ruke i leđa",
                        List.of(
                            ids.get("Warm up"),
                            ids.get("Plyo Push-Ups"),
                            ids.get("Dumbell Row"),
                            ids.get("Lunge dumbell rise"),
                            ids.get("Chest press hip thrust"),
                            ids.get("One leg standing row"),
                            ids.get("One leg standing shoulder press"),
                            ids.get("Unilateral floor chest press"),
                            ids.get("Pull-Ups"),
                            ids.get("Core rotations")
                        )
                    ),
                    new Workout(null, "Full Body", "Strength, Plyo", "Kombinacija svih grupa mišića za potpuni trening.",
                        List.of(
                            ids.get("Barbell Squats"),
                            ids.get("Barbell Reverse Lunge"),
                            ids.get("Landmine Rotation"),
                            ids.get("High Weighted Skipp"),
                            ids.get("Landmine Lateral Lunges"),
                            ids.get("Landmine Explosive Drill"),
                            ids.get("Landmine Shoulder Press"),
                            ids.get("Pull-Ups"),
                            ids.get("Push-Ups")
                        )
                    ),
                    new Workout(null, "Upper Body 2", "Strength", "Vežbe snage za gornji deo tela",
                        List.of(
                            ids.get("Warm up"),
                            ids.get("Plyo Push-Ups")
                        )
                    ),
                    new Workout(null, "Full Body", "Mixed", "Kombinacija svih grupa mišića za potpuni trening.",
                        List.of(ids.get("Push-Ups"), ids.get("Squats"), ids.get("Warm up"))
                    )
                );

                workoutRepository.saveAll(workouts);
                System.out.println("✅ Dodato " + workouts.size() + " treninga u bazu.");
            }
        }
    }
}
