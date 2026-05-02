/*
 * This source file contains application startup and module wiring for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
/**
 * Blog post, reading history, like, bookmark, follow, and author dashboard
 * domain for InkWell.
 *
 * <p>This package owns post persistence and publishing workflows. It also calls
 * category-service for taxonomy synchronization and publishes post events for
 * asynchronous consumers.</p>
 */
package com.inkwell.post;
