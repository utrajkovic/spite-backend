package com.spite.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}
}
@Component
class StartupLogger implements CommandLineRunner {

	@Value("${spring.data.mongodb.database}")
	private String dbName;

	@Override
	public void run(String... args) {
		System.out.println("✅ Connected to MongoDB Atlas (database: " + dbName + ")");
	}
}

