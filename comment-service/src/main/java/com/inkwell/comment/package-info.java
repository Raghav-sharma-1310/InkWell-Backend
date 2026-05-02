/*
 * This source file contains application startup and module wiring for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
/**
 * Comment and comment-like domain for InkWell posts.
 *
 * <p>This package validates authenticated readers, sanitizes comment content,
 * checks post metadata through post-service, persists threaded comments, and
 * emits comment events through messaging.</p>
 */
package com.inkwell.comment;
