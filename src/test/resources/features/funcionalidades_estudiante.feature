Feature: Gestión de Estudiantes
  Como estudiante del sistema
  Quiero poder realizar todas las operaciones disponibles en mi perfil
  Para gestionar mi cuenta, gestionar y ejecutar programas, ver ejemplos, entre otros

  Background:
    Given la API está disponible

  Scenario: Registrar un nuevo estudiantes exitosamente
    When envío una solicitud POST de registro a "/estudiantes" con el siguiente cuerpo:
      """
      {
        "nombre": "andrés",
        "email": "anrumo@gmail.com",
        "contrasena": "Password123"
      }
      """
    Then la respuesta debe tener el código 201
    And la respuesta JSON contiene el campo "message"

  Scenario: Intento de registrar un estudiantes con datos incompletos (faltando nombre)
    When envío una solicitud POST a "/estudiantes" con el siguiente cuerpo:
      """
      {
        "email": "incompleto@example.com",
        "contrasena": "Password123"
      }
      """
    Then la respuesta debe tener el código 400
    And la respuesta JSON contiene el campo "error"

  Scenario: Login exitoso de estudiante
    When envío una solicitud POST a "/estudiantes/login" con el siguiente cuerpo:
      """
      {
        "email": "isabellacardozo11@gmail.com",
        "contrasena": "123"
      }
      """
    Then la respuesta debe tener el código 200
    And la respuesta JSON contiene el campo "token"

  Scenario: Actualizar nombre de un estudiante existente
    Given estoy autenticado como estudiante con email "isabellacardozo11@gmail.com" y contraseña "123"
    And existe un estudiante con email "isabellacardozo11@gmail.com" y  contrasena "123"
    When envío una solicitud PATCH a "/estudiantes/{id}" para el usuario creado con el siguiente cuerpo:
      """
      {
        "nombre": "Isabella C"
      }
      """
    Then la respuesta debe tener el código 200

  Scenario: Obtener todos los programas de estudiantes para revisión
    Given estoy autenticado como estudiante con email "isabellacardozo11@gmail.com" y contraseña "123"
    And existe un estudiante con id "1"
    When envío una solicitud GET a "/estudiantes/1/programas"
    Then la respuesta debe tener el código 200
    And la respuesta es una lista de programas

  Scenario: Intenta eliminar un programa que no existe
    Given estoy autenticado como estudiante con email "isabellacardozo11@gmail.com" y contraseña "123"
    And no existe un programa con id "4"
    When envío una solicitud DELETE a "/estudiantes/programas/4"
    Then la respuesta debe tener el código 404
    And la respuesta JSON contiene el campo "error"