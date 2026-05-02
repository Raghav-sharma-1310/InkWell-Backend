/*
 * This source file contains application startup and module wiring for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
/**
 * Authentication, user profile, role, author-request, feedback, and subscription
 * payment domain for InkWell.
 *
 * <p>This package contains the platform identity boundary. It issues JWTs,
 * stores user accounts and refresh tokens, handles OAuth2 login, sends account
 * emails, manages author requests, and records subscription payment state.</p>
 */
package com.inkwell.auth;
