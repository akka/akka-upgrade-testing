pipeline {
  agent {
      label 'minikube'
  }
  stages {
      stage('Example Build') {
          steps {
              echo 'Hello, Maven'
              sh 'minikube --version'
              sh 'kubectl get pods'
          }
      }
  }
}