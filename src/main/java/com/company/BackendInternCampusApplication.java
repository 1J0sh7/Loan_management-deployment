package com.company;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;



@SpringBootApplication
@EnableRetry
public class BackendInternCampusApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendInternCampusApplication.class, args);
	}

}
