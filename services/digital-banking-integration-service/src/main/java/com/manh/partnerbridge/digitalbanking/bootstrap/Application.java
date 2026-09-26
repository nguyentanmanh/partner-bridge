package com.manh.partnerbridge.digitalbanking.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.manh.partnerbridge.digitalbanking")
public class Application {
    public static void main(String[] args) { SpringApplication.run(Application.class, args); }
}
