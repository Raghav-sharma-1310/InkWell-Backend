/*
 * This source file contains application startup and module wiring for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
/**
 * Razorpay utility service for standalone payment order and verification calls.
 *
 * <p>Most subscription state is stored in auth-service, while this package
 * contains a smaller payment boundary that wraps Razorpay order creation and
 * signature verification.</p>
 */
package com.inkwell.payment;
