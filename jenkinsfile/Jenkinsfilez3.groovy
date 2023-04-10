pipeline {
    agent any
    stages {
        stage('Checkout') {
            steps {
                git 'https://github.com/my-repo.git'
            }
        }
        stage('Deploy to DEV') {
            when {
                changeset "*/backend/**"
                changeset "*/frontend/**"
            }
            steps {
                sh 'docker-compose up -d'
                sh 'sleep 60' // espera 60 segundos para que el servicio se levante completamente
                sh 'curl -f http://localhost:8080/healthcheck' // verifica que el servicio esté disponible
            }
        }
        stage('Test DEV') {
            when {
                branch 'develop'
            }
            steps {
                sh 'curl -f http://localhost:8080/api/v1/test' // ejecuta algunos tests
            }
        }
        stage('Deploy to QA') {
            when {
                branch 'develop'
                allOf {
                    changeset "*/backend/**"
                    changeset "*/frontend/**"
                }
            }
            steps {
                sh 'docker-compose -f docker-compose.qa.yml up -d'
                sh 'sleep 60' // espera 60 segundos para que el servicio se levante completamente
                sh 'curl -f http://localhost:8080/healthcheck' // verifica que el servicio esté disponible
            }
        }
        stage('Test QA') {
            when {
                branch 'develop'
            }
            steps {
                sh 'curl -f http://localhost:8080/api/v1/test' // ejecuta algunos tests
                sh 'curl -f http://localhost:8080' // prueba la UI si está disponible
            }
        }
        stage('Deploy to PROD') {
            when {
                branch 'master'
                allOf {
                    changeset "*/backend/**"
                    changeset "*/frontend/**"
                }
            }
            steps {
                sh 'docker-compose -f docker-compose.prod.yml up -d'
                sh 'sleep 60' // espera 60 segundos para que el servicio se levante completamente
                sh 'curl -f http://localhost:8080/healthcheck' // verifica que el servicio esté disponible
            }
        }
        stage('Test PROD') {
            when {
                branch 'master'
            }
            steps {
                sh 'curl -f http://localhost:8080/api/v1/test' // ejecuta algunos tests
                sh 'curl -f http://localhost:8080' // prueba la UI si está disponible
            }
        }
    }
}
