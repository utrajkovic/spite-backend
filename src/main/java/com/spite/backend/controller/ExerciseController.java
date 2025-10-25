package com.spite.backend.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import com.spite.backend.model.Exercise;
import com.spite.backend.repository.ExerciseRepository;

@RestController
@RequestMapping("/api/exercises")
@CrossOrigin(origins = "*") 
public class ExerciseController {

    private final ExerciseRepository repo;

    public ExerciseController(ExerciseRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Exercise> getAll() {
        return repo.findAll();
    }

    @PostMapping
    public Exercise add(@RequestBody Exercise e) {
        return repo.save(e);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        repo.deleteById(id);
    }

    @PostMapping("/upload")
    public String uploadVideo(@RequestParam("video") MultipartFile file) {
        try {
            Path uploadPath = Paths.get("src/main/resources/uploads/videos/");
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(file.getOriginalFilename());
            Files.write(filePath, file.getBytes());

            System.out.println("✅ Uploadovan fajl: " + filePath.toAbsolutePath());

            return "/videos/" + file.getOriginalFilename();

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("❌ Greška pri uploadu fajla: " + e.getMessage());
        }
    }
}
