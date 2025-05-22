pipeline {
  agent any
  tools {
    jdk 'jdk-23'
    maven 'Maven3'
  }

  environment {
    SONARQUBE_ENV = 'MySonarQube'
  }

  stages {
    stage('Clonar código') {
      steps {
        git branch: 'main', url: 'https://github.com/garojual/PFAPIS.git'
      }
    }

    stage('Ejecutar pruebas') {
      steps {
        echo 'Ejecutando pruebas de integración...'
        sh 'mvn test'
        junit '**/target/cucumber-reports/cucumber.xml'
      }
    }

    stage('Análisis de calidad') {
      steps {
        withSonarQubeEnv("${SONARQUBE_ENV}") {
          sh 'mvn sonar:sonar'
        }
      }
    }
  }
}