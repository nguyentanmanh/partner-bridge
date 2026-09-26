package com.manh.partnerbridge.banking.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.manh.partnerbridge.banking")
public class Application {
    public static void main(String[] args) { SpringApplication.run(Application.class, args); }
}
