pipeline {
    agent any
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/juanjo47/finalproy.git'
                git branch: 'django', url: 'https://github.com/juanjo47/ProyectoDjango.git'
            }
        }
        stage('Deploy to DEV') {
            when {
                changeset "django/"
                changeset "main/"
            }
            steps {
                sh 'docker-compose up -d'
                sh 'sleep 60' // espera 60 segundos para que el servicio se levante completamente
                sh 'curl -f http://localhost:3000' // verifica que el servicio de React esté disponible
                sh 'curl -f http://localhost:8000/admin' // verifica que el servicio de Django esté disponible
            }
        }
        stage('Test DEV') {
            when {
                branch 'develop'
            }
            steps {
                sh 'curl -f http://localhost:3000/api/v1/test' // ejecuta algunos tests de React
                sh 'curl -f http://localhost:8000/api/v1/test' // ejecuta algunos tests de Django
            }
        }
        stage('Deploy to QA') {
            when {
                branch 'develop'
                allOf {
                    changeset "django/"
                    changeset "main/"
                }
            }
            steps {
                sh 'docker-compose -f docker-compose.qa.yml up -d'
                sh 'sleep 60' // espera 60 segundos para que el servicio se levante completamente
                sh 'curl -f http://localhost:3000' // verifica que el servicio de React esté disponible
                sh 'curl -f http://localhost:8000/admin' // verifica que el servicio de Django esté disponible
            }
        }
        stage('Test QA') {
            when {
                branch 'develop'
            }
            steps {
                sh 'curl -f http://localhost:3000/api/v1/test' // ejecuta algunos tests de React
                sh 'curl -f http://localhost:8000/api/v1/test' // ejecuta algunos tests de Django
            }
        }
        stage('Deploy to PROD') {
            when {
                branch 'main'
                allOf {
                    changeset "django/"
                    changeset "main/"
                }
            }
            steps {
                sh 'docker-compose -f docker-compose.prod.yml up -d'
                sh 'sleep 60' // espera 60 segundos para que el servicio se levante completamente
                sh 'curl -f http://localhost:3000' // verifica que el servicio de React esté disponible
                sh 'curl -f http://localhost:8000/admin' // verifica que el servicio de Django esté disponible
            }
        }
        stage('Test PROD') {
            when {
                branch 'main'
            }
            steps {
                sh 'curl -f http://localhost:3000/api/v1/test' // ejecuta algunos tests de React
                sh 'curl -f http://localhost:8000/api/v1/test' // ejecuta algunos tests de Django
            }
        }
    }
}
