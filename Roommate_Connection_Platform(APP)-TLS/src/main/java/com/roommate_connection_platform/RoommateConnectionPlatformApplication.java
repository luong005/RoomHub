package com.roommate_connection_platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RoommateConnectionPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoommateConnectionPlatformApplication.class, args);
    }

}
