# Inkwell Full EC2 Deployment Guide

This guide takes Inkwell from local code to a fully running EC2 deployment with backend microservices, React frontend, Docker Compose, Docker Hub or ECR images, and Jenkins CI/CD.

## 1. What This Project Contains

Backend Spring Boot services:

- `discovery-service`: Eureka service registry, port `8761`
- `admin-server`: Spring Boot Admin, port `9090`
- `api-gateway`: public API entrypoint, port `8080`
- `auth-service`: auth, users, OAuth, JWT, mail, payments bridge, port `8081`
- `post-service`: posts, feeds, follows, bookmarks, port `8082`
- `category-service`: categories and tags, port `8083`
- `comment-service`: comments, port `8084`
- `media-service`: media upload/storage, port `8085`
- `newsletter-service`: subscriptions and campaigns, port `8086`
- `notification-service`: notifications and audit events, port `8087`
- `payment-service`: Razorpay integration, port `8088`

Frontend:

- `frontend-web`: React/Vite app built into static files and served by Nginx on port `80`

Infrastructure:

- MySQL
- Redis
- RabbitMQ
- MailHog for testing mail

## 2. Files To Push To GitHub

Push these files and folders:

- All backend service folders
- `frontend-web`
- `docker-compose.prod.yml`
- `.env.example`
- `Jenkinsfile`
- `docker/mysql-init/01-create-databases.sql`
- `docker/mysql-init/02-create-app-user.sh`
- `docs/deployment-guide.md`
- All `Dockerfile`, `.dockerignore`, `application.yml`, and `application-prod.yml` files
- `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn`

Do not push:

- `.env`
- `target/`
- `node_modules/`
- `frontend-web/dist/`
- `uploads/`
- logs
- real private keys
- real SMTP, OAuth, JWT, database, AWS, Razorpay secrets

`.gitignore` already ignores `.env`, `target/`, `node_modules/`, `dist/`, logs, and uploads.

## 3. Required Accounts And Tools

You need:

- AWS account
- EC2 instance
- Docker Hub account or AWS ECR repository
- GitHub repository
- Jenkins server or Jenkins installed on EC2/another VM
- SSH key pair for EC2
- Optional production domain
- Optional SMTP provider
- Optional AWS S3 bucket for media
- Optional Razorpay, Google OAuth, GitHub OAuth credentials

Recommended EC2 size for all services on one server:

- Minimum: `t3.medium`, 2 vCPU, 4 GB RAM
- Better: `t3.large`, 2 vCPU, 8 GB RAM
- Disk: 30 GB or more
- OS: Ubuntu 22.04/24.04 LTS

## 4. GitHub Push Workflow

Initialize Git if needed:

```bash
git init
git add .
git status
git commit -m "Add production Docker and Jenkins deployment"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
git push -u origin main
```

Before every push, confirm `.env` is not staged:

```bash
git status
git check-ignore .env
```

If `.env` appears in `git status`, stop and remove it:

```bash
git rm --cached .env
```

## 5. Create The EC2 Instance

1. Open AWS Console.
2. Go to EC2.
3. Launch instance.
4. Choose Ubuntu 22.04 or Ubuntu 24.04.
5. Choose `t3.medium` or larger.
6. Create or choose an SSH key pair.
7. Configure storage: at least 30 GB.
8. Create a security group.

Security group inbound rules:

- SSH `22`: your IP only
- HTTP `80`: `0.0.0.0/0`
- HTTPS `443`: `0.0.0.0/0` when TLS is added

Do not publicly open:

- `3306` MySQL
- `6379` Redis
- `5672` RabbitMQ
- `15672` RabbitMQ UI
- `8025` MailHog
- `8761` Eureka
- `9090` Admin Server
- `8080` API Gateway unless debugging temporarily

## 6. Install Docker On EC2

SSH into EC2:

```bash
ssh -i your-key.pem ubuntu@13.48.192.178
```

Install Docker:

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker $USER
newgrp docker
docker --version
docker compose version
```

Create deployment folder:

```bash
sudo mkdir -p /opt/inkwell/docker/mysql-init
sudo chown -R $USER:$USER /opt/inkwell
cd /opt/inkwell
```

## 7. Create The Real EC2 `.env`

Copy `.env.example` from the repo, or copy your local `.env`, into `/opt/inkwell/.env`.

On EC2:

```bash
cd /opt/inkwell
nano .env
chmod 600 .env
```

Generate secrets:

```bash
openssl rand -base64 48
openssl rand -hex 32
```

Minimum values you must replace:

- `DOCKER_IMAGE_PREFIX`
- `FRONTEND_URL`
- `PUBLIC_GATEWAY_URL`
- `OAUTH2_REDIRECT_URI`
- `MEDIA_PUBLIC_BASE_URL`
- `MYSQL_ROOT_PASSWORD`
- `MYSQL_APP_PASSWORD`
- `JWT_SECRET`
- `RABBITMQ_PASSWORD`

Example for Docker Hub:

```env
DOCKER_IMAGE_PREFIX=yourdockerhubuser/inkwell
FRONTEND_URL=http://13.48.192.178
PUBLIC_GATEWAY_URL=http://13.48.192.178
OAUTH2_REDIRECT_URI=http://13.48.192.178/oauth/success
MEDIA_PUBLIC_BASE_URL=http://13.48.192.178/api/media/public/files
```

For first smoke test, you can keep:

```env
MAIL_HOST=mailhog
MAIL_PORT=1025
STORAGE_MODE=local
```

For real production, use:

```env
STORAGE_MODE=s3
AWS_S3_BUCKET=your-bucket
AWS_REGION=ap-south-1
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
AWS_S3_PUBLIC_BASE_URL=https://your-bucket.s3.ap-south-1.amazonaws.com
```

## 8. Docker Hub Setup

Create Docker Hub repositories, or use one namespace prefix:

- `yourdockerhubuser/inkwell/discovery-service`
- `yourdockerhubuser/inkwell/admin-server`
- `yourdockerhubuser/inkwell/api-gateway`
- `yourdockerhubuser/inkwell/auth-service`
- `yourdockerhubuser/inkwell/post-service`
- `yourdockerhubuser/inkwell/comment-service`
- `yourdockerhubuser/inkwell/category-service`
- `yourdockerhubuser/inkwell/media-service`
- `yourdockerhubuser/inkwell/newsletter-service`
- `yourdockerhubuser/inkwell/notification-service`
- `yourdockerhubuser/inkwell/payment-service`
- `yourdockerhubuser/inkwell/frontend-web`

Login locally or in Jenkins:

```bash
docker login
```

## 9. S3 Setup For Media Service

The `media-service` supports two storage modes:

- `STORAGE_MODE=local`: files are stored in the Docker volume `media-uploads`
- `STORAGE_MODE=s3`: files are uploaded to Amazon S3

For production, use S3. The current code uploads files to S3 and saves this URL:

```text
AWS_S3_PUBLIC_BASE_URL/<generated-filename>
```

So your S3 objects must be readable from `AWS_S3_PUBLIC_BASE_URL`. The simple option is a public-read bucket for uploaded blog media. The advanced option is private S3 with CloudFront.

### 9.1 Create An S3 Bucket

Choose one AWS region, for example `ap-south-1`.

Bucket name must be globally unique:

```text
inkwell-media-yourname-prod
```

AWS Console steps:

1. Open AWS Console.
2. Go to S3.
3. Click Create bucket.
4. Bucket name: `inkwell-media-yourname-prod`.
5. Region: same region as your EC2, for example `ap-south-1`.
6. Keep Bucket Versioning off or enable it if you want recovery.
7. For the simple public-media setup, disable "Block all public access".
8. Acknowledge the warning only if this bucket is only for public blog media.
9. Create bucket.

AWS CLI option:

```bash
aws s3api create-bucket \
  --bucket inkwell-media-yourname-prod \
  --region ap-south-1 \
  --create-bucket-configuration LocationConstraint=ap-south-1
```

For `us-east-1`, AWS CLI create-bucket is slightly different:

```bash
aws s3api create-bucket --bucket inkwell-media-yourname-prod --region us-east-1
```

### 9.2 Bucket CORS

Add CORS so browser access works cleanly if images are loaded directly from S3.

Create `s3-cors.json`:

```json
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["GET", "HEAD"],
    "AllowedOrigins": [
      "http://13.48.192.178",
      "https://YOUR_DOMAIN"
    ],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3000
  }
]
```

Apply it:

```bash
aws s3api put-bucket-cors \
  --bucket inkwell-media-yourname-prod \
  --cors-configuration file://s3-cors.json
```

### 9.3 Simple Public Bucket Policy

Use this only for files that are meant to be public, such as post images and avatars.

Create `s3-public-read-policy.json`:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadUploadedMedia",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::inkwell-media-yourname-prod/*"
    }
  ]
}
```

Apply it:

```bash
aws s3api put-bucket-policy \
  --bucket inkwell-media-yourname-prod \
  --policy file://s3-public-read-policy.json
```

Public URL format:

```text
https://inkwell-media-yourname-prod.s3.ap-south-1.amazonaws.com
```

Use that value as:

```env
AWS_S3_PUBLIC_BASE_URL=https://inkwell-media-yourname-prod.s3.ap-south-1.amazonaws.com
```

### 9.4 Create IAM Access For Media Service

Best practice is an EC2 IAM role. Simpler setup is an IAM user access key.

Recommended permissions for the media service:

- Upload files: `s3:PutObject`
- Delete files: `s3:DeleteObject`
- Read objects for verification: `s3:GetObject`
- List bucket for debugging: `s3:ListBucket`

IAM policy:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ListMediaBucket",
      "Effect": "Allow",
      "Action": "s3:ListBucket",
      "Resource": "arn:aws:s3:::inkwell-media-yourname-prod"
    },
    {
      "Sid": "ManageMediaObjects",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::inkwell-media-yourname-prod/*"
    }
  ]
}
```

IAM user option:

1. Go to IAM.
2. Create policy using the JSON above.
3. Create user: `inkwell-media-service`.
4. Attach the policy.
5. Create an access key for application use.
6. Put the access key values only in EC2 `/opt/inkwell/.env`.

EC2 role option:

1. Go to IAM.
2. Create role.
3. Trusted entity: AWS service.
4. Use case: EC2.
5. Attach the S3 policy above.
6. Attach the role to your EC2 instance.

Important: this project currently reads explicit `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` from `.env` when `STORAGE_MODE=s3`. So the easiest fully working setup is the IAM user access key in `.env`. To use an EC2 role only, update `media-service` so `S3Client` uses the AWS default credentials provider chain instead of requiring explicit keys.

### 9.5 EC2 `.env` Values For S3

Edit `/opt/inkwell/.env`:

```env
STORAGE_MODE=s3
AWS_S3_BUCKET=inkwell-media-yourname-prod
AWS_REGION=ap-south-1
AWS_ACCESS_KEY_ID=YOUR_IAM_ACCESS_KEY
AWS_SECRET_ACCESS_KEY=YOUR_IAM_SECRET_KEY
AWS_S3_PUBLIC_BASE_URL=https://inkwell-media-yourname-prod.s3.ap-south-1.amazonaws.com
MEDIA_PUBLIC_BASE_URL=https://inkwell-media-yourname-prod.s3.ap-south-1.amazonaws.com
```

Restart only the media service:

```bash
cd /opt/inkwell
docker compose -f docker-compose.prod.yml --env-file .env up -d media-service
docker compose -f docker-compose.prod.yml --env-file .env logs -f media-service
```

### 9.6 How Media Gets Pushed To S3

You do not manually push user media to S3 during normal app usage.

Flow:

1. User uploads media from frontend.
2. Frontend sends file to `POST /api/media/author/upload` or `POST /api/media/user/upload-avatar`.
3. Nginx forwards `/api/...` to `api-gateway`.
4. Gateway routes to `media-service`.
5. `media-service` uploads the file to S3 using `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`.
6. `media-service` stores metadata in MySQL.
7. Frontend receives the public S3 URL and displays it.

Manual test upload to S3:

```bash
aws s3 cp ./test-image.jpg s3://inkwell-media-yourname-prod/test-image.jpg
```

Open:

```text
https://inkwell-media-yourname-prod.s3.ap-south-1.amazonaws.com/test-image.jpg
```

### 9.7 Verify From EC2

Install AWS CLI if needed:

```bash
sudo apt-get update
sudo apt-get install -y unzip curl
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install
aws --version
```

Check credentials from `.env` manually:

```bash
cd /opt/inkwell
set -a
source .env
set +a
aws s3 ls s3://$AWS_S3_BUCKET --region $AWS_REGION
```

Check media-service logs:

```bash
docker compose -f docker-compose.prod.yml --env-file .env logs -f media-service
```

Test through the app:

1. Open frontend: `http://13.48.192.178`.
2. Login as an author.
3. Upload an image.
4. Confirm it appears in S3:

```bash
aws s3 ls s3://$AWS_S3_BUCKET --recursive --region $AWS_REGION
```

### 9.8 Advanced: Private S3 With CloudFront

For stronger production security:

1. Keep S3 Block Public Access enabled.
2. Create a CloudFront distribution.
3. Use Origin Access Control for S3.
4. Allow CloudFront to read bucket objects.
5. Set:

```env
AWS_S3_PUBLIC_BASE_URL=https://YOUR_CLOUDFRONT_DOMAIN
MEDIA_PUBLIC_BASE_URL=https://YOUR_CLOUDFRONT_DOMAIN
```

The media service will still upload to S3, but users read files through CloudFront.

## 10. Manual Build And Push

From your project root:

```bash
./mvnw -B clean verify
```

Build frontend:

```bash
cd frontend-web
npm ci
npm exec vitest run
npm run build
cd ..
```

Build images:

```bash
docker build -t YOUR_PREFIX/discovery-service:latest ./discovery-service
docker build -t YOUR_PREFIX/admin-server:latest ./admin-server
docker build -t YOUR_PREFIX/api-gateway:latest ./api-gateway
docker build -t YOUR_PREFIX/auth-service:latest ./auth-service
docker build -t YOUR_PREFIX/post-service:latest ./post-service
docker build -t YOUR_PREFIX/comment-service:latest ./comment-service
docker build -t YOUR_PREFIX/category-service:latest ./category-service
docker build -t YOUR_PREFIX/media-service:latest ./media-service
docker build -t YOUR_PREFIX/newsletter-service:latest ./newsletter-service
docker build -t YOUR_PREFIX/notification-service:latest ./notification-service
docker build -t YOUR_PREFIX/payment-service:latest ./payment-service
docker build --build-arg VITE_API_BASE_URL=/ -t YOUR_PREFIX/frontend-web:latest ./frontend-web
```

Push images:

```bash
docker push YOUR_PREFIX/discovery-service:latest
docker push YOUR_PREFIX/admin-server:latest
docker push YOUR_PREFIX/api-gateway:latest
docker push YOUR_PREFIX/auth-service:latest
docker push YOUR_PREFIX/post-service:latest
docker push YOUR_PREFIX/comment-service:latest
docker push YOUR_PREFIX/category-service:latest
docker push YOUR_PREFIX/media-service:latest
docker push YOUR_PREFIX/newsletter-service:latest
docker push YOUR_PREFIX/notification-service:latest
docker push YOUR_PREFIX/payment-service:latest
docker push YOUR_PREFIX/frontend-web:latest
```

## 11. Manual EC2 Deployment

Copy these files to EC2:

```bash
scp docker-compose.prod.yml ubuntu@13.48.192.178:/opt/inkwell/
scp docker/mysql-init/* ubuntu@13.48.192.178:/opt/inkwell/docker/mysql-init/
```

On EC2:

```bash
cd /opt/inkwell
docker compose -f docker-compose.prod.yml --env-file .env pull
docker compose -f docker-compose.prod.yml --env-file .env up -d
docker compose -f docker-compose.prod.yml --env-file .env ps
```

If you need a clean restart:

```bash
docker compose -f docker-compose.prod.yml --env-file .env down
docker compose -f docker-compose.prod.yml --env-file .env up -d
```

Do not delete volumes unless you want to delete databases/uploads:

```bash
docker volume ls
```

## 12. Jenkins Setup

Install Jenkins on a separate server or EC2 instance. The Jenkins agent must have:

- Java
- Git
- Docker
- Docker Compose plugin
- Node.js 22
- npm
- Maven can use project `mvnw`
- SSH access to EC2

Install Jenkins plugins:

- Pipeline
- Git
- GitHub
- Credentials Binding
- SSH Agent
- Docker Pipeline, optional

Create Jenkins credentials:

- `dockerhub-credentials`: username/password or access token for Docker Hub
- `ec2-ssh-key`: SSH private key for EC2 user, usually `ubuntu`

Create a Jenkins Pipeline job:

1. New Item
2. Pipeline
3. Pipeline script from SCM
4. SCM: Git
5. Repository URL: your GitHub repo URL
6. Branch: `main`
7. Script path: `Jenkinsfile`

Set Jenkins build parameters:

- `EC2_HOST`: `ubuntu@13.48.192.178`
- `EC2_APP_DIR`: `/opt/inkwell`
- `VITE_API_BASE_URL`: `http://13.48.192.178:8080`

Before first Jenkins deploy, ensure EC2 already has:

- Docker installed
- `/opt/inkwell/.env`
- Docker login if images are private, or Jenkins must deploy credentials another way

The Jenkins pipeline will:

1. Pull code from GitHub.
2. Build backend with Maven.
3. Run backend tests.
4. Run frontend tests.
5. Build Docker images.
6. Push Docker images.
7. SSH into EC2.
8. Copy compose/init files.
9. Pull latest images on EC2.
10. Run compose down/up.
11. Remove unused Docker images.

## 13. Frontend Deployment Flow

The frontend is deployed as a Docker image:

- Jenkins runs `docker build ./frontend-web`.
- Vite builds static files.
- Nginx serves `/`.
- Nginx proxies `/api`, `/oauth2`, and `/login` to `api-gateway:8080` inside Docker.

That means the browser opens:

```text
http://13.48.192.178
```

And frontend API calls go through same-origin Nginx:

```text
http://13.48.192.178/api/...
```

Use this in `.env`:

```env
VITE_API_BASE_URL=/
FRONTEND_URL=http://13.48.192.178
PUBLIC_GATEWAY_URL=http://13.48.192.178
```

## 14. Service Communication Rules

Inside Docker, services must use Docker service names, not `localhost`:

- MySQL: `mysql`
- Redis: `redis`
- RabbitMQ: `rabbitmq`
- MailHog: `mailhog`
- Eureka: `discovery-service`
- Admin server: `admin-server`
- Gateway: `api-gateway`

`localhost` is only valid inside a container for that same container, so it is used only in container health checks.

## 15. Verification Commands

Check containers:

```bash
cd /opt/inkwell
docker compose -f docker-compose.prod.yml --env-file .env ps
```

Check frontend:

```bash
curl -I http://13.48.192.178
curl http://13.48.192.178/health
```

Check gateway from EC2:

```bash
curl http://127.0.0.1:8080/actuator/health
```

Check admin server:

```bash
curl http://127.0.0.1:9090/actuator/health
```

Check Eureka:

```bash
curl http://127.0.0.1:8761/eureka/apps
```

Check infrastructure:

```bash
docker compose -f docker-compose.prod.yml --env-file .env exec mysql mysqladmin ping -uroot -p
docker compose -f docker-compose.prod.yml --env-file .env exec redis redis-cli ping
docker compose -f docker-compose.prod.yml --env-file .env exec rabbitmq rabbitmq-diagnostics -q ping
```

View logs:

```bash
docker compose -f docker-compose.prod.yml --env-file .env logs -f api-gateway
docker compose -f docker-compose.prod.yml --env-file .env logs -f auth-service
docker compose -f docker-compose.prod.yml --env-file .env logs -f frontend-web
```

Check app in browser:

```text
http://13.48.192.178
```

## 16. Debugging

If a service is unhealthy:

```bash
docker compose -f docker-compose.prod.yml --env-file .env ps
docker inspect CONTAINER_NAME --format '{{json .State.Health}}'
docker compose -f docker-compose.prod.yml --env-file .env logs SERVICE_NAME
```

If database connection fails:

- Check `MYSQL_APP_USER`
- Check `MYSQL_APP_PASSWORD`
- Check MySQL container logs
- Confirm `02-create-app-user.sh` ran on first MySQL startup

If OAuth fails:

- Check OAuth callback URL in Google/GitHub console
- It must match `PUBLIC_GATEWAY_URL/login/oauth2/code/google` or GitHub equivalent
- Check `OAUTH2_REDIRECT_URI`

If CORS fails:

- Check `FRONTEND_URL`
- It must exactly match the browser origin
- Include `http://` or `https://`

If frontend cannot call backend:

- Check `frontend-web/nginx.conf`
- Check `api-gateway` is healthy
- Check browser network tab for `/api/...`

## 17. Admin And Debug UIs

Admin/debug tools are bound to EC2 localhost only. Use SSH tunnels:

```bash
ssh -i your-key.pem \
  -L 9090:127.0.0.1:9090 \
  -L 8761:127.0.0.1:8761 \
  -L 15672:127.0.0.1:15672 \
  -L 8025:127.0.0.1:8025 \
  ubuntu@13.48.192.178
```

Then open locally:

- Spring Boot Admin: `http://127.0.0.1:9090`
- Eureka: `http://127.0.0.1:8761`
- RabbitMQ UI: `http://127.0.0.1:15672`
- MailHog UI: `http://127.0.0.1:8025`

## 18. Production Hardening

Before real users:

- Add HTTPS with domain and TLS.
- Replace MailHog with SMTP provider.
- Use S3 for media.
- Rotate old secrets that were previously present in config history.
- Restrict SSH to your IP.
- Do not open database/cache/message broker ports publicly.
- Use strong passwords and a long random JWT secret.
- Back up MySQL volumes.
- Consider RDS instead of container MySQL for serious production.
- Consider Amazon MQ/ElastiCache instead of container RabbitMQ/Redis.
- Monitor disk usage with `df -h`.
- Prune images periodically with `docker image prune -f`.

## 19. Common Release Flow

Developer workflow:

```bash
git status
git add .
git commit -m "Your change"
git push origin main
```

Jenkins workflow:

```text
Build Now -> Jenkins builds/tests/images -> pushes images -> deploys to EC2
```

EC2 workflow:

```bash
cd /opt/inkwell
docker compose -f docker-compose.prod.yml --env-file .env ps
docker compose -f docker-compose.prod.yml --env-file .env logs -f
```

Rollback:

1. Set `IMAGE_TAG` in `/opt/inkwell/.env` to a previous Jenkins build number.
2. Run:

```bash
docker compose -f docker-compose.prod.yml --env-file .env pull
docker compose -f docker-compose.prod.yml --env-file .env up -d
```

## 20. Final Checklist

Before deployment:

- GitHub repo has code and deployment files.
- `.env` is not in GitHub.
- Docker images are pushed.
- EC2 has Docker installed.
- EC2 has `/opt/inkwell/.env`.
- EC2 security group opens `80` and `22` only.
- Jenkins credentials exist.
- Jenkins parameters are correct.

After deployment:

- `frontend-web` is healthy.
- `api-gateway` is healthy.
- All backend services are registered in Eureka.
- MySQL, Redis, RabbitMQ are healthy.
- Login/register works.
- Post creation works.
- Comments work.
- Media upload works.
- Newsletter/mail flow works.
- Notifications work.
- Payment flow works if Razorpay keys are configured.
