package com.manh.partnerbridge.securities.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.manh.partnerbridge.securities")
public class Application {
    public static void main(String[] args) { SpringApplication.run(Application.class, args); }
}
