package com.arsenal.lebanon.manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ArsenalLebanonSupportersClubManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArsenalLebanonSupportersClubManagerApplication.class, args);
    }

}
