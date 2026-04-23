package org.example.talktripchattingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TalktripChattingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TalktripChattingServiceApplication.class, args);
    }

}
