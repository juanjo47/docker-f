pipeline {
    agent any
    environment {
        // Definir las variables de entorno necesarias para el despliegue
        BACKEND_IMAGE = 'backend-image'
        FRONTEND_IMAGE = 'frontend-image'
        DOCKER_REGISTRY = 'dockerhub.com'
        K8S_NAMESPACE = 'my-namespace'
        K8S_CLUSTER_NAME = 'my-cluster'
    }
    stages {
        stage('Checkout') {
            steps {
                // Clonar el repositorio de Git
                git 'https://github.com/my-repo.git'
            }
        }
        stage('Build Backend') {
            steps {
                // Construir la imagen del backend
                sh 'docker build -t $DOCKER_REGISTRY/$BACKEND_IMAGE .'
            }
        }
        stage('Push Backend') {
            steps {
                // Subir la imagen del backend al registro de Docker
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                    sh "docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD $DOCKER_REGISTRY"
                }
                sh "docker push $DOCKER_REGISTRY/$BACKEND_IMAGE"
            }
        }
        stage('Build Frontend') {
            steps {
                // Construir la aplicación de frontend
                sh 'npm install'
                sh 'npm run build'
            }
        }
        stage('Push Frontend') {
            steps {
                // Subir la aplicación de frontend a un bucket de S3
                withCredentials([awsCredentials(credentialsId: 'aws-creds', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    sh "aws s3 cp dist/ s3://my-bucket/ --recursive"
                }
            }
        }
        stage('Deploy Backend') {
            steps {
                // Desplegar la imagen del backend en un clúster de Kubernetes
                withKubeConfig([credentialsId: 'kubeconfig-creds', serverUrl: "https://api.$K8S_CLUSTER_NAME.example.com"]) {
                    sh "kubectl set image deployment/my-backend-deployment backend=$DOCKER_REGISTRY/$BACKEND_IMAGE -n $K8S_NAMESPACE"
                }
            }
        }
        stage('Deploy Frontend') {
            steps {
                // Desplegar la aplicación de frontend en un clúster de Kubernetes
                withKubeConfig([credentialsId: 'kubeconfig-creds', serverUrl: "https://api.$K8S_CLUSTER_NAME.example.com"]) {
                    sh "kubectl apply -f kubernetes/frontend.yaml -n $K8S_NAMESPACE"
                }
            }
        }
    }
}