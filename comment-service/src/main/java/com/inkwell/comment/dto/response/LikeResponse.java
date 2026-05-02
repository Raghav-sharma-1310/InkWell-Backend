/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.comment.dto.response;

public record LikeResponse(boolean liked, long likesCount) {}
