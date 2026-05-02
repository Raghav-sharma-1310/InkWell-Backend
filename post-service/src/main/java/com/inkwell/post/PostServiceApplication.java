/*
 * This source file contains application startup and module wiring for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableFeignClients
@SpringBootApplication
/* This class groups post service application behavior so the module keeps a clear responsibility. */
public class PostServiceApplication {

    // Defines main so related behavior stays grouped in one place.
    public static void main(String[] args) {
        SpringApplication.run(PostServiceApplication.class, args);
    }
}
