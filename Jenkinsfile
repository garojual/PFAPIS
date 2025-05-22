pipeline {
  agent any // O un agente específico con soporte para Docker
  tools {
    jdk 'jdk-23'
    maven 'Maven3'
  }

  environment {
    SONARQUBE_ENV = 'MySonarQube'
    // Define la URL de la BD accesible desde el agente
    // Si levantas el DB container en el agente, puede que necesites usar 'localhost'
    // o 'host.docker.internal' (si el agente es un contenedor) y mapear el puerto.
    // O si usas un network, el nombre del contenedor DB.
    // Esta es la parte TRICKY: cómo el app en 'mvn quarkus:dev' ve la DB.
    DB_URL = 'jdbc:postgresql://localhost:5432/bd_uq' // Ejemplo: asumiendo mapeo de puerto
    DB_USER = 'root'
    DB_PASSWORD = 'root'

    // URL donde la app se levantará (localhost en el agente)
    APP_BASE_URL = 'http://localhost:8080'
  }

  stages {
    stage('Clonar código') {
      steps {
        git branch: 'main', url: 'https://github.com/garojual/PFAPIS.git'
      }
    }

    stage('Levantar Base de Datos (en contenedor sidecar)') {
        steps {
            echo 'Levantando contenedor de base de datos...'
            // Nombre para el contenedor DB
            def dbContainerName = "postgres-test-jenkins-${env.BUILD_NUMBER}"
            script {
                // Intenta detener y remover si ya existía de una ejecución anterior fallida
                sh "docker stop ${dbContainerName} || true"
                sh "docker rm ${dbContainerName} || true"
                // Levanta un nuevo contenedor de PostgreSQL
                sh """
                  docker run -d --name ${dbContainerName} \\
                  -e POSTGRES_DB=${DB_URL.split('/').last()} \\
                  -e POSTGRES_USER=${DB_USER} \\
                  -e POSTGRES_PASSWORD=${DB_PASSWORD} \\
                  -p 5432:5432 \\
                  postgres:15
                """
                // Espera a que la BD esté lista (esto puede requerir lógica más robusta)
                echo 'Esperando a que la base de datos inicie...'
                sh 'sleep 15' // Ajusta si tu BD tarda más
                // O usa un loop con pg_isready si docker exec está disponible y postgres tools instaladas
                // sh "docker exec ${dbContainerName} pg_isready -U ${DB_USER} -d ${DB_URL.split('/').last()}"
            }
            echo 'Base de datos levantada.'
        }
    }


    stage('Levantar Quarkus') {
          steps {
            echo 'Iniciando Quarkus en segundo plano...'
            // Pasar las propiedades de la BD a mvn quarkus:dev
            sh """
              nohup mvn quarkus:dev \\
              -Dquarkus.datasource.jdbc.url=${DB_URL} \\
              -Dquarkus.datasource.username=${DB_USER} \\
              -Dquarkus.datasource.password=${DB_PASSWORD} \\
              > quarkus.log 2>&1 & echo \$! > quarkus.pid
            """
            echo 'Esperando a que Quarkus inicie...'
            sh 'sleep 20' // ¡¡¡AJUSTA MUCHO ESTO!!! Quarkus tarda en levantar y conectar a la BD
            // Considera usar un script que espere a que el endpoint health check responda 200
            // sh 'while ! curl -s http://localhost:8080/q/health | grep "UP"; do sleep 5; done' // Si tienes health check
          }
        }


    stage('Ejecutar pruebas') {
      steps {
        echo 'Ejecutando pruebas de integración...'
        // Pasa la URL de la aplicación a las pruebas (localhost:8080)
        sh "mvn test -Dapp.base.url=${APP_BASE_URL}"
        //junit '**/target/surefire-reports/TEST-*.xml'
        junit '**/target/cucumber-reports/cucumber.xml'
      }
    }

    stage('Análisis de calidad') {
      steps {
        // Asumiendo que SonarQube es accesible desde el agente
        withSonarQubeEnv("${SONARQUBE_ENV}") {
          sh 'mvn sonar:sonar'
        }
      }
    }
  }

  post {
    always {
        echo 'Limpieza final: Deteniendo Quarkus y Base de Datos...'
        // Detener Quarkus
        sh '''
          if [ -f quarkus.pid ]; then
            kill -9 $(cat quarkus.pid) || true
            rm quarkus.pid
          fi
        '''
        // Detener y eliminar el contenedor DB
        def dbContainerName = "postgres-test-jenkins-${env.BUILD_NUMBER}"
        sh "docker stop ${dbContainerName} || true"
        sh "docker rm ${dbContainerName} || true"
        echo 'Limpieza completa.'
    }
  }
}