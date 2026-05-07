// Jenkins declarative pipeline for building, publishing, and deploying Inkwell.
// Required credentials are documented in docs/deployment-guide.md.

pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
  }

  parameters {
    string(name: 'EC2_HOST', defaultValue: 'ec2-user@your-ec2-public-dns', description: 'SSH target for the EC2 deployment host')
    string(name: 'EC2_APP_DIR', defaultValue: '/opt/inkwell', description: 'Directory on EC2 containing .env and compose files')
    string(name: 'VITE_API_BASE_URL', defaultValue: 'http://16.171.23.137:8080', description: 'Frontend API base URL baked into the Vite build')
  }

  environment {
    DOCKER_USERNAME = credentials('dockerhub-username')
    DOCKER_PASSWORD = credentials('dockerhub-password')
    BACKEND_REPO = 'https://github.com/Raghav-sharma-1310/InkWell-Backend.git'
    FRONTEND_REPO = 'https://github.com/Raghav-sharma-1310/InkWell-Frontend.git'
    IMAGE_TAG = "${env.BUILD_NUMBER}"
    COMPOSE_FILE = 'docker-compose.prod.yml'
  }

  stages {
    stage('Checkout Backend And Frontend') {
      steps {
        script {
          deleteDir()
        }
        dir('backend') {
          git branch: 'main', url: "${BACKEND_REPO}"
        }
        dir('frontend') {
          git branch: 'main', url: "${FRONTEND_REPO}"
        }
      }
    }

    stage('Build Backend') {
      steps {
        dir('backend') {
          sh '''
            chmod +x mvnw
            ./mvnw -B clean verify
          '''
        }
      }
    }

    stage('Build Frontend') {
      steps {
        dir('frontend') {
          sh '''
            if [ -f package-lock.json ]; then
              npm ci
            else
              npm install
            fi
            
            npm run build
          '''
        }
      }
    }

    stage('Docker Login') {
      steps {
        sh 'echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin'
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
            sh "docker build -t ${DOCKER_USERNAME}/inkwell-${service}:${env.IMAGE_TAG} -t ${DOCKER_USERNAME}/inkwell-${service}:latest ./backend/${service}"
          }

          sh "docker build --build-arg VITE_API_BASE_URL=${params.VITE_API_BASE_URL} -t ${DOCKER_USERNAME}/inkwell-frontend:${env.IMAGE_TAG} -t ${DOCKER_USERNAME}/inkwell-frontend:latest ./frontend"
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
            'payment-service'
          ]

          services.each { service ->
            sh "docker push ${DOCKER_USERNAME}/inkwell-${service}:${env.IMAGE_TAG}"
            sh "docker push ${DOCKER_USERNAME}/inkwell-${service}:latest"
          }
          
          sh "docker push ${DOCKER_USERNAME}/inkwell-frontend:${env.IMAGE_TAG}"
          sh "docker push ${DOCKER_USERNAME}/inkwell-frontend:latest"
        }
      }
    }

    stage('Deploy All On EC2') {
      steps {
        sshagent(credentials: ['ec2-ssh-key']) {
          sh "ssh -o StrictHostKeyChecking=no ${params.EC2_HOST} 'mkdir -p ${params.EC2_APP_DIR}/docker/mysql-init'"
          // Use the compose files from the cloned backend repo
          sh "scp -o StrictHostKeyChecking=no backend/${COMPOSE_FILE} ${params.EC2_HOST}:${params.EC2_APP_DIR}/${COMPOSE_FILE}"
          sh "scp -o StrictHostKeyChecking=no backend/docker/mysql-init/* ${params.EC2_HOST}:${params.EC2_APP_DIR}/docker/mysql-init/"
          sh """
            ssh -o StrictHostKeyChecking=no ${params.EC2_HOST} '
              set -eu
              cd ${params.EC2_APP_DIR}
              test -f .env
              sed -i "s/^IMAGE_TAG=.*/IMAGE_TAG=${env.IMAGE_TAG}/" .env
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
    success {
      echo 'Inkwell backend and frontend deployment successful.'
    }
    failure {
      echo 'Deployment failed. Check Jenkins console logs.'
    }
    always {
      sh 'docker logout || true'
    }
  }
}
