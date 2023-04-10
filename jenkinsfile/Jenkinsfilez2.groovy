parameters {
    choice(
        choices: ['DEV', 'QA', 'PROD'],
        description: 'Seleccione el ambiente de destino',
        name: 'ENVIRONMENT'
    )
}

pipeline {
    agent any
    environment {
        DOCKER_REGISTRY = 'dockerhub.com'
        BACKEND_IMAGE = "my-backend-image-${params.ENVIRONMENT.toLowerCase()}"
        FRONTEND_IMAGE = "my-frontend-image-${params.ENVIRONMENT.toLowerCase()}"
        K8S_NAMESPACE = "my-namespace-${params.ENVIRONMENT.toLowerCase()}"
        K8S_CLUSTER_NAME = "my-cluster-${params.ENVIRONMENT.toLowerCase()}"
    }
    stages {
        stage('Checkout') {
            steps {
                git 'https://github.com/my-repo.git'
            }
        }
        stage('Build Backend') {
            steps {
                sh 'docker build -t $DOCKER_REGISTRY/$BACKEND_IMAGE .'
            }
        }
        stage('Push Backend') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                    sh "docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD $DOCKER_REGISTRY"
                }
                sh "docker push $DOCKER_REGISTRY/$BACKEND_IMAGE"
            }
        }
        stage('Build Frontend') {
            steps {
                sh 'npm install'
                sh 'npm run build'
            }
        }
        stage('Push Frontend') {
            steps {
                withCredentials([awsCredentials(credentialsId: 'aws-creds', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    sh "aws s3 cp dist/ s3://my-bucket/ --recursive"
                }
            }
        }
        stage('Deploy Backend') {
            steps {
                withKubeConfig([credentialsId: 'kubeconfig-creds', serverUrl: "https://api.$K8S_CLUSTER_NAME.example.com"]) {
                    sh "kubectl set image deployment/my-backend-deployment backend=$DOCKER_REGISTRY/$BACKEND_IMAGE -n $K8S_NAMESPACE"
                }
            }
        }
        stage('Deploy Frontend') {
            steps {
                withKubeConfig([credentialsId: 'kubeconfig-creds', serverUrl: "https://api.$K8S_CLUSTER_NAME.example.com"]) {
                    sh "kubectl apply -f kubernetes/frontend.yaml -n $K8S_NAMESPACE"
                }
            }
        }
    }
}