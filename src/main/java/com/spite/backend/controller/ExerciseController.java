package com.spite.backend.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

import com.spite.backend.model.Exercise;
import com.spite.backend.repository.ExerciseRepository;
import com.spite.backend.service.CloudinaryService;

@RestController
@RequestMapping("/api/exercises")
@CrossOrigin(origins = "*")
public class ExerciseController {

    private final ExerciseRepository repo;
    private final CloudinaryService cloudinaryService;

    public ExerciseController(ExerciseRepository repo, CloudinaryService cloudinaryService) {
        this.repo = repo;
        this.cloudinaryService = cloudinaryService;
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
            String videoUrl = cloudinaryService.uploadVideo(file);
            System.out.println("✅ Uploadovano na Cloudinary: " + videoUrl);
            return videoUrl; // frontend dobija cloud URL
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("❌ Greška pri uploadu videa: " + e.getMessage());
        }
    }
}
