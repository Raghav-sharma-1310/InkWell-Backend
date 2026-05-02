# Run Guide

## 1. After Unzipping, Change These First

Edit `.env` from `.env.example` and check:

- `JWT_SECRET`
- `MYSQL_ROOT_PASSWORD`
- `PUBLIC_GATEWAY_URL` and `FRONTEND_APP_URL`
- `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` if you want Google OAuth2
- `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` if you want GitHub OAuth2
- `OAUTH2_REDIRECT_URI`
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`
- `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS`
- `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET` if you want real checkout
- `ADMIN_SERVER_PORT` if `9090` is already used
- `STORAGE_MODE`
- `AWS_*` values only if you want S3 instead of local file storage

For local demo, you can leave OAuth2, SMTP, Razorpay, and S3 values empty. The project will still run with email delivery skipped and payments handled in demo mode.

## 2. Prerequisites

- Java 21
- Docker Desktop
- Node.js 22+

## 3. Backend With Docker

1. Copy `.env.example` to `.env`
2. Run `docker compose up --build`
3. Open:
   - Gateway: `http://localhost:8080`
   - Eureka: `http://localhost:8761`
   - Admin Server: `http://localhost:9090`
   - RabbitMQ UI: `http://localhost:15672`

## 4. Frontend Locally

1. Go to `frontend-web`
2. Run `npm install`
3. Run `npm run dev`
4. Open `http://localhost:5173`

## 5. Backend Locally Without Docker

1. Start MySQL, Redis, RabbitMQ, and your SMTP provider if you want mail delivery
2. Run each service with the Maven wrapper:
   - `mvnw.cmd spring-boot:run -pl discovery-service`
   - `mvnw.cmd spring-boot:run -pl admin-server`
   - `mvnw.cmd spring-boot:run -pl api-gateway`
   - `mvnw.cmd spring-boot:run -pl auth-service`
   - `mvnw.cmd spring-boot:run -pl post-service`
   - `mvnw.cmd spring-boot:run -pl category-service`
   - `mvnw.cmd spring-boot:run -pl comment-service`
   - `mvnw.cmd spring-boot:run -pl media-service`
   - `mvnw.cmd spring-boot:run -pl newsletter-service`
   - `mvnw.cmd spring-boot:run -pl notification-service`

## 6. Demo Accounts

- Admin: `admin@inkwell.dev` / `Admin@123`
- Author: `author@inkwell.dev` / `Author@123`
- Reader: `reader@inkwell.dev` / `Reader@123`

## 7. OAuth Callback Values

If you open the app locally from the same machine:

- Google authorized JavaScript origin: `http://localhost:5173`
- Google redirect URI: `http://localhost:8080/login/oauth2/code/google`
- GitHub callback URL: `http://localhost:8080/login/oauth2/code/github`
- Frontend success redirect: `http://localhost:5173/oauth/success`

If you open the app through a LAN IP such as `http://10.0.0.136`, replace every `localhost` above with that same IP and also update:

- `PUBLIC_GATEWAY_URL`
- `FRONTEND_APP_URL`
- `OAUTH2_REDIRECT_URI`

Use one hostname consistently across browser, OAuth console settings, and `.env`, otherwise Google/GitHub sign-in will fail with redirect URI mismatch errors.
