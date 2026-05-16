package com.fileapproval;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FileApprovalApplication {
    public static void main(String[] args) {
        SpringApplication.run(FileApprovalApplication.class, args);
    }
}
