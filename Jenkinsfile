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

    stage('Levantar Quarkus') {
          steps {
            echo 'Iniciando Quarkus en segundo plano...'
            sh 'nohup mvn quarkus:dev > quarkus.log 2>&1 & echo $! > quarkus.pid'
            sh 'sleep 10' // Ajusta el tiempo si el servidor tarda más en levantar
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

    stage('Detener Quarkus') {
          steps {
            echo 'Deteniendo Quarkus...'
            sh '''
              if [ -f quarkus.pid ]; then
                kill -9 $(cat quarkus.pid) || true
                rm quarkus.pid
              fi
            '''
          }
        }
      }

      post {
        always {
          echo 'Limpieza final'
          sh 'pkill -f quarkus:dev || true'
        }
      }


  }
}