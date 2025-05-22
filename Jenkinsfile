pipeline {
  agent any // O un agente específico con soporte para Docker
  tools {
    jdk 'jdk-23'
    maven 'Maven3'
  }

  environment {
    SONARQUBE_ENV = 'MySonarQube'
    // Define la URL de la BD accesible desde el agente
    // Si levantas el DB container en el agente con -p 5432:5432, 'localhost' suele funcionar.
    DB_URL = 'jdbc:postgresql://localhost:5432/bd_uq' // Ejemplo: asumiendo mapeo de puerto 5432
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
            script { // <-- Envuelve el código Groovy en un bloque script
                echo 'Levantando contenedor de base de datos...'
                // Nombre para el contenedor DB
                def dbContainerName = "postgres-test-jenkins-${env.BUILD_NUMBER}" // Ahora válido dentro de script

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
                // **MUY IMPORTANTE:** Este sleep puede ser insuficiente o demasiado largo.
                // Considera una espera más robusta, como intentar conectar a la BD o usar pg_isready
                // si tienes docker exec y los comandos ps aux/pg_isready disponibles en el agente/contenedor DB.
                // Ejemplo (requiere docker exec y postgres client en agente o check contra puerto 5432):
                // sh 'while ! nc -z localhost 5432; do sleep 2; done' // Requiere netcat en el agente
                // sh 'docker exec ${dbContainerName} pg_isready -U ${DB_USER} -d ${DB_URL.split("/").last()}' // Requiere docker exec y postgres client en el contenedor DB
                 sh 'sleep 20' // Ajusta este tiempo si es necesario
            } // <-- Fin del bloque script
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
            echo 'Esperando a que Quarkus inicie y conecte a la BD...'
            // **MUY IMPORTANTE:** Este sleep es CRÍTICO y DEBE ser suficiente
            // para que Quarkus levante, conecte a la BD y esté listo para recibir peticiones.
            // Si tus pruebas siguen dando 403, INCREMENTA este tiempo o implementa un check de salud.
            // Ejemplo de health check (si tienes endpoint /q/health/live):
            // sh 'while ! curl -s http://localhost:8080/q/health/live | grep "UP"; do echo "Quarkus not ready yet, waiting..."; sleep 5; done'
            sh 'sleep 30' // Ajusta este tiempo. 10 segundos es probable que sea MUY POCO.
            echo 'Quarkus iniciado (asumiendo que el tiempo de espera fue suficiente).'
          }
        }


    stage('Ejecutar pruebas') {
      steps {
        echo "Ejecutando pruebas de integración apuntando a ${APP_BASE_URL}..."
        // Pasa la URL de la aplicación a las pruebas (localhost:8080)
        // Asegúrate de que tus pruebas lean esta propiedad o variable de entorno
        sh "mvn test -Dapp.base.url=${APP_BASE_URL}"
        // Asegúrate de que la ruta al reporte JUnit sea correcta
        junit '**/target/surefire-reports/TEST-*.xml' // Reporte estándar de Surefire
        // Si usas Cucumber, puede que necesites:
        // junit '**/target/cucumber-reports/cucumber.xml'
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
        script { // <-- Envuelve el código Groovy en un bloque script
            echo 'Limpieza final: Deteniendo Quarkus y Base de Datos...'
            // Detener Quarkus
            sh '''
              if [ -f quarkus.pid ]; then
                kill -9 $(cat quarkus.pid) || true
                rm quarkus.pid
              fi
            '''
            // Detener y eliminar el contenedor DB
            def dbContainerName = "postgres-test-jenkins-${env.BUILD_NUMBER}" // Ahora válido dentro de script
            sh "docker stop ${dbContainerName} || true"
            sh "docker rm ${dbContainerName} || true"
            echo 'Limpieza completa.'
        } // <-- Fin del bloque script
    }
  }
}