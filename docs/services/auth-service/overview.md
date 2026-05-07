# Auth Service — Service Overview

## Purpose
The Auth Service is the **central identity and access management** service. It handles user registration, login, OAuth2 social login, JWT token management, role-based access, subscription/payment processing, feedback management, author requests, and the Admin Console.

## Port: 8081 | Database: auth_db

---

## Controllers

| Class | Base Path | Purpose |
|---|---|---|
| `AuthController` | `/api/auth` | Registration, login, logout, refresh, password reset, profile management |
| `AdminUserController` | `/api/auth/admin/users` | Admin: list users, update roles, suspend/reactivate, delete users |
| `AdminConsoleController` | `/api/auth/admin/console` | Default Admin: remove admin role, delete admin accounts |
| `AdminAuthorRequestController` | `/api/admin/author-requests` | Admin: approve/reject author role requests |
| `AdminFeedbackController` | `/api/admin/feedback` | Admin: manage bug reports |
| `AuthorRequestController` | `/api/author-request` | Users: submit author role request |
| `FeedbackController` | `/api/feedback` | Users: submit and view bug reports |
| `InternalUserController` | `/api/auth/internal` | Internal: fetch user by ID (used by notification-service) |
| `PublicUserController` | `/api/auth/public` | Public: search users, get authors list |
| `PaymentController` | `/api/auth/payments` | Subscription payment: create order, verify payment |
| `ServiceInfoController` | `/` | Service info endpoint |

## Services

| Class | Purpose |
|---|---|
| `AuthService` | Core auth logic: register, login, refresh, password reset, role updates, user CRUD |
| `AdminConsoleService` | Default admin privileges: demote/delete other admins |
| `AuthorRequestService` | Author role request workflow |
| `EmailService` | Sends transactional emails (welcome, login notification, OTP, verification) |
| `FeedbackService` | Bug report & feedback message management |
| `LoginRateLimiter` | In-memory login attempt throttling |
| `OAuth2AccountService` | Handles Google/GitHub OAuth2 user provisioning |
| `OtpService` | OTP generation and verification for password reset |
| `PaymentGatewayClient` | Razorpay API integration |
| `PaymentService` | Subscription order creation and payment verification |
| `RefreshTokenService` | JWT refresh token lifecycle |
| `AuditLogService` | Audit trail logging |

## Repositories

| Class | Entity | Purpose |
|---|---|---|
| `UserRepository` | User | User CRUD + search queries |
| `RefreshTokenRepository` | RefreshToken | Refresh token management |
| `EmailVerificationTokenRepository` | EmailVerificationToken | Email verification tokens |
| `PasswordOtpRepository` | PasswordOtp | OTP storage |
| `PaymentOrderRepository` | PaymentOrder | Payment order records |
| `AuditLogRepository` | AuditLog | Audit trail storage |
| `AuthorRequestRepository` | AuthorRequest | Author role requests |
| `FeedbackReportRepository` | FeedbackReport | Bug reports |

## Entities

| Class | Table | Key Fields |
|---|---|---|
| `User` | users | userId, username, email, passwordHash, role, provider, active, subscriptionTier |
| `RefreshToken` | refresh_tokens | token, user, expiryDate, revoked |
| `EmailVerificationToken` | email_verification_tokens | token, user, used |
| `PasswordOtp` | password_otps | email, otp, expiryTime, used |
| `PaymentOrder` | payment_orders | gatewayOrderId, amount, status, provider |
| `AuditLog` | audit_logs | actor, action, entityType, details |
| `AuthorRequest` | author_requests | userId, status, reason |
| `FeedbackReport` | feedback_reports | subject, description, status, priority |
| `FeedbackMessage` | feedback_messages | reportId, senderId, content |

## Enums
`Role` (READER, AUTHOR, ADMIN), `AuthProvider` (LOCAL, GOOGLE, GITHUB), `SubscriptionTier` (FREE, PRO), `SubscriptionStatus`, `PaymentStatus`, `PaymentGatewayProvider`, `RequestStatus`, `FeedbackStatus`

## DTOs
**Requests**: RegisterRequest, LoginRequest, RoleUpdateRequest, UpdateProfileRequest, ChangePasswordRequest
**Responses**: AuthResponse, UserResponse, ProfileResponse, PaymentOrderResponse, PaymentVerifyResponse, FeedbackReportResponse, FeedbackMessageResponse

## Security Classes

| Class | Purpose |
|---|---|
| `SecurityConfig` | HTTP security chain: CSRF disabled, OAuth2 login, gateway filter |
| `GatewayAuthenticationFilter` | Reads X-User-* headers → builds GatewayUserPrincipal |
| `GatewayUserPrincipal` | Record: userId, username, email, role |
| `JwtService` | JWT generation (access token) |
| `OAuth2SuccessHandler` | Redirect after successful OAuth2 login |

## Config Classes

| Class | Purpose |
|---|---|
| `AdminSeeder` | Seeds default admin account (admin@inkwell.dev) on startup |
| `DataInitializer` | Seeds demo data |

## Mapper
`UserMapper` — MapStruct mapper: `User` → `UserResponse`

---

## APIs Exposed

### Public (no auth required)
| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login |
| POST | `/api/auth/refresh` | Refresh JWT |
| POST | `/api/auth/forgot-password` | Send OTP |
| POST | `/api/auth/verify-otp` | Verify OTP |
| POST | `/api/auth/reset-password` | Reset password |
| GET | `/api/auth/public/search` | Search users |
| GET | `/api/auth/public/authors` | List authors |

### Authenticated
| Method | Path | Description |
|---|---|---|
| GET | `/api/auth/me` | Get current user |
| PUT | `/api/auth/me` | Update profile |
| POST | `/api/auth/change-password` | Change password |
| DELETE | `/api/auth/me` | Deactivate account |
| POST | `/api/auth/logout` | Logout |

### Admin Only
| Method | Path | Description |
|---|---|---|
| GET | `/api/auth/admin/users` | List all users |
| PATCH | `/api/auth/admin/users/{id}/role` | Update user role |
| PATCH | `/api/auth/admin/users/{id}/suspend` | Suspend user |
| PATCH | `/api/auth/admin/users/{id}/reactivate` | Reactivate user |
| DELETE | `/api/auth/admin/users/{id}` | Delete user |

### Default Admin Only
| Method | Path | Description |
|---|---|---|
| PUT | `/api/auth/admin/console/admins/{id}/remove-role` | Demote admin to READER |
| DELETE | `/api/auth/admin/console/admins/{id}` | Delete admin account |

---

## External Tools
- **MySQL**: auth_db schema
- **SMTP/Mailpit**: Transactional email delivery
- **Google/GitHub**: OAuth2 social login
- **Razorpay**: Payment processing
