package com.spite.backend.service;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

@Service
public class AsyncCompressionService {

    @Autowired
    private BunnyCDNService bunnyCDNService;

    @Async
    public void compressAndReplace(File inputFile, String fileName) {
        try {
            File outputFile = File.createTempFile("output_", ".mp4");

            FFmpeg ffmpeg = new FFmpeg("/usr/bin/ffmpeg");
            FFprobe ffprobe = new FFprobe("/usr/bin/ffprobe");

            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(inputFile.getAbsolutePath())
                    .overrideOutputFiles(true)
                    .addOutput(outputFile.getAbsolutePath())
                    .setFormat("mp4")
                    .setVideoCodec("libx264")
                    .setVideoBitRate(500_000)
                    .addExtraArgs("-preset", "ultrafast")
                    .addExtraArgs("-vf", "scale=480:-2")
                    .addExtraArgs("-an")
                    .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                    .done();

            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
            executor.createJob(builder).run();

            bunnyCDNService.uploadToStorage(outputFile, fileName);
            System.out.println("✅ Compressed and replaced: " + fileName + " (" + outputFile.length() / 1024 + "KB)");

            inputFile.delete();
            outputFile.delete();

        } catch (Exception e) {
            System.err.println("❌ Async compression failed: " + e.getMessage());
            inputFile.delete();
        }
    }
}