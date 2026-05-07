# User Service — Service Overview

> **Note**: InkWell does **not** have a separate "user-service" module. All user management functionality — registration, profile updates, role management, user search, author profiles — is handled within the **auth-service** (port 8081, database auth_db).

This document describes the user-management subsystem within auth-service.

## Purpose
Manages the complete user lifecycle: registration, authentication, profile management, role assignments, suspension, deletion, and public user/author profiles.

## Port: 8081 (part of auth-service) | Database: auth_db

---

## Controllers (User-Related)

| Class | Base Path | Purpose |
|---|---|---|
| `AuthController` | `/api/auth` | Registration, login, profile CRUD |
| `AdminUserController` | `/api/auth/admin/users` | Admin: list, update role, suspend, delete |
| `AdminConsoleController` | `/api/auth/admin/console` | Default Admin: manage other admins |
| `PublicUserController` | `/api/auth/public` | Public: search users, list authors |
| `InternalUserController` | `/api/auth/internal` | Internal: get user by ID (Feign) |

## Services (User-Related)

| Class | Purpose |
|---|---|
| `AuthService` | User CRUD, role updates, search, profile |
| `AdminConsoleService` | Default admin privileges |

## Repository
`UserRepository` — JPA repository for User entity

## Entity
`User` — see auth-service documentation for full field listing

## Role Hierarchy
```
READER → AUTHOR → ADMIN → DEFAULT_ADMIN (admin@inkwell.dev)
```

- **READER**: Default role. Can read, like, comment
- **AUTHOR**: Can create/publish posts, upload media
- **ADMIN**: Can manage users, moderate content, broadcast
- **DEFAULT_ADMIN**: admin@inkwell.dev — can demote/delete other admins

## User Flows

### Registration
1. User submits email + password → AuthService.register()
2. User entity created with role=READER, provider=LOCAL, active=true, subscriptionTier=FREE
3. Welcome email sent

### Profile Update
1. Authenticated user calls PUT /api/auth/me
2. AuthService.updateProfile() updates fullName, bio, avatarUrl, phoneNumber

### Role Promotion
1. Admin calls PATCH /api/auth/admin/users/{id}/role with new role
2. AuthService.updateRole() changes user's role

### Admin Demotion (Default Admin Only)
1. Default admin calls PUT /api/auth/admin/console/admins/{id}/remove-role
2. AdminConsoleService.removeAdminRole() changes admin's role to READER

---

## Diagrams
See auth-service diagrams — user management is fully contained within auth-service.
