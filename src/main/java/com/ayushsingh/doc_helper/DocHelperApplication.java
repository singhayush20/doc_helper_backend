package com.ayushsingh.doc_helper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableCaching
@EnableMongoAuditing
public class DocHelperApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocHelperApplication.class, args);
	}
}
