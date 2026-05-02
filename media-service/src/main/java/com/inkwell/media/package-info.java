/*
 * This source file contains application startup and module wiring for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
/**
 * Media upload and storage metadata domain for InkWell.
 *
 * <p>This package validates upload requests, stores files through a pluggable
 * storage abstraction, and persists metadata so posts and profiles can reference
 * public media URLs.</p>
 */
package com.inkwell.media;
