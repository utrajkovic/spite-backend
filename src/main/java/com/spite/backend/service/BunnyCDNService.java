package com.spite.backend.service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

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
        // Sačuvaj original privremeno
        File inputFile = File.createTempFile("input_", "_" + file.getOriginalFilename());
        file.transferTo(inputFile);

        // Kompresuj sa ffmpeg
        File outputFile = File.createTempFile("output_", ".mp4");

        try {
            FFmpeg ffmpeg = new FFmpeg("/usr/bin/ffmpeg");
            FFprobe ffprobe = new FFprobe("/usr/bin/ffprobe");

                FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(inputFile.getAbsolutePath())
                    .overrideOutputFiles(true)
                    .addOutput(outputFile.getAbsolutePath())
                    .setFormat("mp4")
                    .setVideoCodec("libx264")
                    .setVideoBitRate(500_000)
                    .addExtraArgs("-vf", "scale=480:-2")
                    .addExtraArgs("-an")
                    .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                    .done();

            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
            executor.createJob(builder).run();

        } catch (Exception e) {
            System.err.println("❌ FFmpeg compression failed: " + e.getMessage());
            e.printStackTrace();
            outputFile = inputFile;
        }

        // Upload na BunnyCDN
        String fileName = "videos/" + UUID.randomUUID() + ".mp4";
        String uploadUrl = "https://storage.bunnycdn.com/" + storageZoneName + "/" + fileName;

        byte[] videoBytes = Files.readAllBytes(outputFile.toPath());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("AccessKey", apiKey)
                .header("Content-Type", "application/octet-stream")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(videoBytes))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Počisti temp fajlove
        inputFile.delete();
        if (!outputFile.equals(inputFile)) outputFile.delete();

        if (response.statusCode() != 201) {
            throw new IOException("BunnyCDN upload failed: " + response.statusCode() + " " + response.body());
        }

        System.out.println("✅ Video uploaded to BunnyCDN: " + cdnUrl + "/" + fileName);
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
}