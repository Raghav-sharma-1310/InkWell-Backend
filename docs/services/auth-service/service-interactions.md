# Auth Service — Service Interactions

## Inbound (receives requests from)

| Source | Protocol | Endpoint | Purpose |
|---|---|---|---|
| API Gateway | HTTP/REST | All `/api/auth/**` routes | Frontend API requests |
| notification-service | HTTP/REST (Feign) | `GET /api/auth/internal/users/{userId}` | Fetch user email for notification emails |
| notification-service | HTTP/REST (Feign) | `GET /api/auth/public/search?query=` | List all users for admin broadcasts |

## Outbound (sends requests to)

| Target | Protocol | Class | Data Exchanged | Purpose |
|---|---|---|---|---|
| Google OAuth2 | HTTPS | Spring Security OAuth2 | Authorization code → user profile | Social login |
| GitHub OAuth2 | HTTPS | Spring Security OAuth2 | Authorization code → user profile | Social login |
| Razorpay API | HTTPS | `PaymentGatewayClient` | Order creation + payment verification | Subscription payments |
| Mailpit/SMTP | SMTP | `EmailService` | HTML emails | Welcome, login, OTP, verification emails |

## RabbitMQ
No direct interaction. Auth Service does not publish or consume RabbitMQ messages.

## Redis
No direct interaction found in active code. (Config exists in application.yml but no cache usage.)

## Key Data Flows

### 1. User Registration
`Frontend → Gateway → AuthController.register() → AuthService → UserRepository.save() → EmailService.sendWelcomeEmail()`

### 2. OAuth2 Login
`Frontend → Gateway → /login/oauth2/code/{provider} → OAuth2AccountService.loadUser() → OAuth2SuccessHandler → redirect to frontend with tokens`

### 3. Payment Verification
`Frontend → Gateway → PaymentController.verifyPayment() → PaymentService → PaymentGatewayClient (Razorpay) → Update user subscription tier`

### 4. Admin Console
`Frontend → Gateway (ADMIN role check) → AdminConsoleController → AdminConsoleService (default admin email check) → UserRepository`
