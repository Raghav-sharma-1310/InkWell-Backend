/*
 * This source file contains Spring Boot configuration for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.category.config;

import com.inkwell.category.entity.Category;
import com.inkwell.category.entity.Tag;
import com.inkwell.category.repository.CategoryRepository;
import com.inkwell.category.repository.TagRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
/* This class groups data initializer behavior so the module keeps a clear responsibility. */
public class DataInitializer implements CommandLineRunner {
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    @Override
    // Defines run so related behavior stays grouped in one place.
    public void run(String... args) {
        if (categoryRepository.count() == 0) {
            categoryRepository.saveAll(List.of(
                Category.builder().name("Engineering").slug("engineering").description("Backend, frontend, and platform engineering").postCount(0L).build(),
                Category.builder().name("Productivity").slug("productivity").description("Workflows, writing, and creator efficiency").postCount(0L).build()
            ));
        }
        if (tagRepository.count() == 0) {
            tagRepository.saveAll(List.of(
                Tag.builder().name("Spring Boot").slug("spring-boot").postCount(0L).build(),
                Tag.builder().name("Microservices").slug("microservices").postCount(0L).build(),
                Tag.builder().name("React").slug("react").postCount(0L).build()
            ));
        }
    }
}
