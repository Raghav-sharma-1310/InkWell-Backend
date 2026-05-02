/*
 * This source file contains application startup and module wiring for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
/**
 * Edge service for routing, JWT validation, request enrichment, and gateway
 * level policies.
 *
 * <p>The gateway is the public backend entry point. It validates bearer tokens,
 * forwards identity headers to downstream services, applies route predicates,
 * and discovers service instances through Eureka.</p>
 */
package com.inkwell.gateway;
