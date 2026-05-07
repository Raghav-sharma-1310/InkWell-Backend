# Mailpit (MailHog Replacement) Usage in InkWell

## Overview
Mailpit (`axllent/mailpit`) is used as a **development email server** that captures all outgoing emails without actually delivering them. It replaces MailHog in modern setups.

## Configuration
- **Image**: `axllent/mailpit:latest`
- **SMTP Port**: 1025 (receives emails)
- **Web UI Port**: 8025 (view captured emails)
- **URL**: http://localhost:8025

## Usage by Service

### 1. Auth Service — Transactional Emails
- **Class**: `com.inkwell.auth.service.EmailService`
- **Emails sent**:
  - Welcome email (on registration)
  - Login notification
  - Password reset OTP
  - Email verification links
  - Subscription confirmation
- **Config**: `spring.mail.host: ${MAIL_HOST:smtp.gmail.com}` (Mailpit in Docker: `mailpit:1025`)

### 2. Newsletter Service — Campaign Emails
- **Class**: `com.inkwell.newsletter.service.MailService`
- **Emails sent**:
  - Subscription confirmation (double opt-in)
  - Campaign broadcasts to subscribers
- **Config**: `spring.mail.host: ${MAIL_HOST:smtp.gmail.com}` (Mailpit in Docker: `mailpit:1025`)

### 3. Notification Service — Notification Emails
- **Class**: `com.inkwell.notification.service.MailService`
- **Emails sent**:
  - Comment notification emails
  - Reply notification emails
  - Admin broadcast emails
- **Config**: `spring.mail.host: ${MAIL_HOST:localhost}` (Mailpit in Docker: `mailpit:1025`)

## How It Works in Development
1. Services send emails via JavaMail to SMTP port 1025
2. Mailpit captures all emails (never delivers to real addresses)
3. Developers view captured emails at http://localhost:8025
4. In production, `MAIL_HOST` is set to a real SMTP server (e.g., Gmail, SES)

## Services NOT Using Mailpit
- api-gateway, post-service, comment-service, category-service, media-service, payment-service, discovery-service, admin-server: No direct interaction
