/*
 * This source file contains application startup and module wiring for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.admin;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableAdminServer
@SpringBootApplication
/* This class groups admin server application behavior so the module keeps a clear responsibility. */
public class AdminServerApplication {

    // Defines main so related behavior stays grouped in one place.
    public static void main(String[] args) {
        SpringApplication.run(AdminServerApplication.class, args);
    }
}