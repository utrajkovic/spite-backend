package com.spite.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${cloudinary.cloud_name}") String cloudName,
            @Value("${cloudinary.api_key}") String apiKey,
            @Value("${cloudinary.api_secret}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret));
    }

    public String uploadVideo(MultipartFile file) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", "video",
                        "transformation", new Transformation()
                                .quality("auto:eco")
                                .fetchFormat("mp4")));

        return uploadResult.get("secure_url").toString();
    }

    public String upload(MultipartFile file, Map<String, Object> options) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
        return uploadResult.get("secure_url").toString();
    }

    public boolean deleteVideo(String videoUrl) {
        try {
            String[] parts = videoUrl.split("/");
            String fileName = parts[parts.length - 1];
            String publicId = fileName.substring(0, fileName.lastIndexOf(".")); // skini .mp4

            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", "video",
                    "invalidate", true));

            return "ok".equals(result.get("result"));
        } catch (Exception e) {
            System.err.println("❌ Error deleting from Cloudinary: " + e.getMessage());
            return false;
        }
    }
}
