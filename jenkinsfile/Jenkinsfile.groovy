pipeline {
    agent any
    environment {
        IMAGE_NAME = 'my-image'
        DOCKER_REGISTRY = 'docker.io'
        DOCKER_USERNAME = credentials('docker-hub-username')
        DOCKER_PASSWORD = credentials('docker-hub-password')
    }
    stages {
        stage('Build backend') {
            steps {
                dir('backend') {
                    sh 'pip install -r requirements.txt'
                    sh 'python manage.py test'
                    sh 'docker build -t $DOCKER_REGISTRY/$DOCKER_USERNAME/$IMAGE_NAME-backend:latest .'
                }
            }
        }
        stage('Build frontend') {
            steps {
                dir('frontend') {
                    sh 'npm install'
                    sh 'npm run build'
                    sh 'docker build -t $DOCKER_REGISTRY/$DOCKER_USERNAME/$IMAGE_NAME-frontend:latest .'
                }
            }
        }
        stage('Push images') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-hub-login', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                    sh 'docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD $DOCKER_REGISTRY'
                    sh 'docker push $DOCKER_REGISTRY/$DOCKER_USERNAME/$IMAGE_NAME-backend:latest'
                    sh 'docker push $DOCKER_REGISTRY/$DOCKER_USERNAME/$IMAGE_NAME-frontend:latest'
                }
            }
        }
        stage('Deploy') {
            when {
                branch 'master'
            }
            steps {
                sh 'docker-compose up -d'
            }
        }
    }
}
