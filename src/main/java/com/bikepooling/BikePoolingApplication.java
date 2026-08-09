package com.bikepooling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BikePoolingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BikePoolingApplication.class, args);
    }

}
