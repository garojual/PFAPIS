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
          sleep 20
          echo "PostgreSQL debería estar disponible"
        '''
      }
    }

    stage('Levantar Quarkus') {
      steps {
        echo 'Iniciando Quarkus en segundo plano...'
        sh '''
          # Limpiar procesos anteriores
          pkill -f quarkus:dev || true

          # Configurar variables de entorno para la conexión a BD
          export QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://postgres1:5432/bd_uq
          export QUARKUS_HTTP_HOST=0.0.0.0

          # Iniciar Quarkus
          nohup mvn quarkus:dev -Dquarkus.http.host=0.0.0.0 > quarkus.log 2>&1 & echo $! > quarkus.pid

          # Esperar más tiempo
          sleep 45

          # Verificar que el proceso está corriendo
          if [ -f quarkus.pid ]; then
            PID=$(cat quarkus.pid)
            if ps -p $PID > /dev/null 2>&1; then
              echo "Quarkus iniciado (PID: $PID)"
            else
              echo "ERROR: Quarkus no se inició"
              cat quarkus.log
              exit 1
            fi
          fi

          # Mostrar los últimos logs para debug
          echo "=== ÚLTIMOS LOGS DE QUARKUS ==="
          tail -30 quarkus.log
        '''
      }
    }

    stage('Debug BD') {
      steps {
        echo 'Verificando contenido de la base de datos...'
        sh '''
          # Instalar cliente PostgreSQL
          apt-get update && apt-get install -y postgresql-client || echo "Ya está instalado"

          # Verificar conectividad y contenido
          PGPASSWORD=root psql -h postgres1 -U root -d bd_uq -c "\\dt" || echo "No se pudo conectar"
          PGPASSWORD=root psql -h postgres1 -U root -d bd_uq -c "SELECT * FROM profesores LIMIT 5;" || echo "Tabla profesores no existe o está vacía"
        '''
      }
    }

    stage('Ejecutar pruebas') {
      steps {
        echo 'Ejecutando pruebas de integración...'
        sh '''
          # Configurar la misma URL de BD para los tests
          export QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://postgres1:5432/bd_uq

          echo "=== ANTES DE EJECUTAR TESTS ==="
          echo "Verificando conectividad..."

          # Verificar que Quarkus responde (sin fallar si no responde)
          curl -v http://localhost:8080/ || echo "Quarkus no responde en /"

          echo "=== EJECUTANDO TESTS ==="
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
    failure {
      echo 'Pipeline falló - Mostrando logs de debug'
      sh '''
        echo "=== LOGS DE QUARKUS ==="
        cat quarkus.log || echo "No hay logs de Quarkus"
        echo "=== PROCESOS JAVA ==="
        ps aux | grep java || echo "No hay procesos Java"
      '''
    }
  }
}