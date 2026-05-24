package com.spite.backend.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BunnyCDNService {

    @Value("${bunnycdn.storage.api_key}")
    private String apiKey;

    @Value("${bunnycdn.storage.zone_name}")
    private String storageZoneName;

    @Value("${bunnycdn.cdn.url}")
    private String cdnUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String uploadVideo(MultipartFile file) throws IOException, InterruptedException {
        String extension = getExtension(file.getOriginalFilename());
        String fileName = "videos/" + UUID.randomUUID() + "." + extension;

        String uploadUrl = "https://storage.bunnycdn.com/" + storageZoneName + "/" + fileName;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("AccessKey", apiKey)
                .header("Content-Type", "application/octet-stream")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 201) {
            throw new IOException("BunnyCDN upload failed: " + response.statusCode() + " " + response.body());
        }

        return cdnUrl + "/" + fileName;
    }

    public boolean deleteVideo(String videoUrl) {
        try {
            String fileName = videoUrl.replace(cdnUrl + "/", "");
            String deleteUrl = "https://storage.bunnycdn.com/" + storageZoneName + "/" + fileName;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(deleteUrl))
                    .header("AccessKey", apiKey)
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Error deleting from BunnyCDN: " + e.getMessage());
            return false;
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "mp4";
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}