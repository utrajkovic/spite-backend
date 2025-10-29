package com.spite.backend.controller;

import org.springframework.http.ResponseEntity;
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

@PostMapping(
    value = "/upload",
    consumes = { "multipart/form-data", "video/*", "*/*" }
)
public ResponseEntity<String> uploadVideo(@RequestParam("video") MultipartFile file) {
    try {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file received by the server.");
        }

        String contentType = file.getContentType();
        System.out.println("📥 File primljen: " + file.getOriginalFilename() + " | MIME: " + contentType);

        // ✅ Dozvoli mp4, mov i video/*
        if (contentType == null ||
            !(contentType.equalsIgnoreCase("video/mp4") ||
              contentType.equalsIgnoreCase("video/quicktime") ||
              contentType.startsWith("video"))) {
            return ResponseEntity.badRequest().body("Unsupported file type: " + contentType);
        }

        String videoUrl = cloudinaryService.uploadVideo(file);
        System.out.println("✅ Uploadovano na Cloudinary: " + videoUrl);

        return ResponseEntity
                .ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "POST, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type")
                .body(videoUrl);

    } catch (IOException e) {
        e.printStackTrace();
        return ResponseEntity.internalServerError().body("Upload failed: " + e.getMessage());
    }
}


}
