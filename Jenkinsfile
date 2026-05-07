// Jenkins declarative pipeline for building, publishing, and deploying Inkwell.
// Required credentials are documented in docs/deployment-guide.md.

pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
  }

  parameters {
    string(name: 'DOCKER_IMAGE_PREFIX', defaultValue: 'your-dockerhub-user/inkwell', description: 'Docker image prefix, for example dockerhub-user/inkwell or AWS_ACCOUNT.dkr.ecr.REGION.amazonaws.com/inkwell')
    string(name: 'EC2_HOST', defaultValue: 'ec2-user@your-ec2-public-dns', description: 'SSH target for the EC2 deployment host')
    string(name: 'EC2_APP_DIR', defaultValue: '/opt/inkwell', description: 'Directory on EC2 containing .env and compose files')
    string(name: 'VITE_API_BASE_URL', defaultValue: '/', description: 'Frontend API base URL baked into the Vite build')
  }

  environment {
    IMAGE_TAG = "${env.BUILD_NUMBER}"
    COMPOSE_FILE = 'docker-compose.prod.yml'
    // CI-only placeholders let tests start without committing development secrets.
    DB_PASSWORD = 'ci-db-password'
    JWT_SECRET = 'ci-jwt-secret-ci-jwt-secret-ci-jwt-secret-ci-jwt-secret'
  }

  stages {
    stage('Checkout') {
      steps {
        // Pull the latest source from the GitHub repository configured for this Jenkins job.
        checkout scm
      }
    }

    stage('Backend Build And Tests') {
      steps {
        // Maven builds every Spring Boot module and runs unit tests.
        sh './mvnw -B clean verify'
      }
    }

    stage('Frontend Tests') {
      steps {
        dir('frontend-web') {
          // npm ci keeps CI installs reproducible from package-lock.json.
          sh 'npm ci'
          sh 'npm exec vitest run'
        }
      }
    }

    stage('Docker Login') {
      steps {
        withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
          // For AWS ECR, replace this with: aws ecr get-login-password | docker login --username AWS --password-stdin <registry>
          sh 'echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin'
        }
      }
    }

    stage('Build Docker Images') {
      steps {
        script {
          def services = [
            'discovery-service',
            'admin-server',
            'api-gateway',
            'auth-service',
            'post-service',
            'comment-service',
            'category-service',
            'media-service',
            'newsletter-service',
            'notification-service',
            'payment-service'
          ]

          services.each { service ->
            sh "docker build -t ${params.DOCKER_IMAGE_PREFIX}/${service}:${env.IMAGE_TAG} -t ${params.DOCKER_IMAGE_PREFIX}/${service}:latest ./${service}"
          }

          sh "docker build --build-arg VITE_API_BASE_URL=${params.VITE_API_BASE_URL} -t ${params.DOCKER_IMAGE_PREFIX}/frontend-web:${env.IMAGE_TAG} -t ${params.DOCKER_IMAGE_PREFIX}/frontend-web:latest ./frontend-web"
        }
      }
    }

    stage('Push Docker Images') {
      steps {
        script {
          def services = [
            'discovery-service',
            'admin-server',
            'api-gateway',
            'auth-service',
            'post-service',
            'comment-service',
            'category-service',
            'media-service',
            'newsletter-service',
            'notification-service',
            'payment-service',
            'frontend-web'
          ]

          services.each { service ->
            sh "docker push ${params.DOCKER_IMAGE_PREFIX}/${service}:${env.IMAGE_TAG}"
            sh "docker push ${params.DOCKER_IMAGE_PREFIX}/${service}:latest"
          }
        }
      }
    }

    stage('Deploy To EC2') {
      steps {
        sshagent(credentials: ['ec2-ssh-key']) {
          // Keep deployment config in source control, while .env lives only on the EC2 host.
          sh "ssh -o StrictHostKeyChecking=no ${params.EC2_HOST} 'mkdir -p ${params.EC2_APP_DIR}/docker/mysql-init'"
          sh "scp -o StrictHostKeyChecking=no ${COMPOSE_FILE} ${params.EC2_HOST}:${params.EC2_APP_DIR}/${COMPOSE_FILE}"
          sh "scp -o StrictHostKeyChecking=no docker/mysql-init/* ${params.EC2_HOST}:${params.EC2_APP_DIR}/docker/mysql-init/"
          sh """
            ssh -o StrictHostKeyChecking=no ${params.EC2_HOST} '
              set -eu
              cd ${params.EC2_APP_DIR}
              test -f .env
              docker compose -f ${COMPOSE_FILE} --env-file .env pull
              docker compose -f ${COMPOSE_FILE} --env-file .env down
              docker compose -f ${COMPOSE_FILE} --env-file .env up -d
              docker image prune -f
            '
          """
        }
      }
    }
  }

  post {
    always {
      // Keep the Jenkins agent tidy after image builds.
      sh 'docker logout || true'
    }
  }
}
