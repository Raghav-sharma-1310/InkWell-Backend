/*
 * This source file contains automated verification for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.notification;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/* This class groups notification service application tests behavior so the module keeps a clear responsibility. */
class NotificationServiceApplicationTests {

    @Test
    void applicationClassExists() {
        assertThat(NotificationServiceApplication.class).isNotNull();
    }
}
