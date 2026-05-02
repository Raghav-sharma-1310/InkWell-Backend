/*
 * This source file contains application startup and module wiring for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
/**
 * Eureka discovery server for the InkWell platform.
 *
 * <p>This package owns service registration and lookup. Other services register
 * here so the API Gateway and service clients can resolve logical service names
 * instead of hard-coded host and port combinations.</p>
 */
package com.inkwell.discovery;
