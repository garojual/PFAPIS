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

    stage('Levantar servicios') {
      steps {
        echo 'Verificando disponibilidad de servicios...'
        sh '''
          echo "Esperando a que PostgreSQL esté disponible..."
          # Usar telnet en lugar de nc, o simplemente esperar un tiempo fijo
          sleep 20
          echo "Asumiendo que PostgreSQL está disponible"
        '''
      }
    }

    stage('Levantar Quarkus') {
      steps {
        echo 'Iniciando Quarkus en segundo plano...'
        sh '''
          export QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://postgres1:5432/bd_uq
          nohup mvn quarkus:dev -Dquarkus.http.host=0.0.0.0 > quarkus.log 2>&1 & echo $! > quarkus.pid
          sleep 15

          # Verificar que Quarkus esté disponible
          until curl -f http://localhost:8080/q/health || nc -z localhost 8080; do
            echo "Esperando Quarkus..."
            sleep 2
          done
          echo "Quarkus está disponible"
        '''
      }
    }

    stage('Ejecutar pruebas') {
      steps {
        echo 'Ejecutando pruebas de integración...'
        sh '''
          export QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://postgres1:5432/bd_uq
          mvn test -Dquarkus.http.test-port=8080 -Dquarkus.test.continuous-testing=disabled
        '''
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