/*
 * This source file contains application startup and module wiring for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.newsletter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
/* This class groups newsletter service application behavior so the module keeps a clear responsibility. */
public class NewsletterServiceApplication {
    // Defines main so related behavior stays grouped in one place.
    public static void main(String[] args) { SpringApplication.run(NewsletterServiceApplication.class, args); }
}
