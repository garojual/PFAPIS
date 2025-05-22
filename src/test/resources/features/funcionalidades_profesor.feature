Feature: Gestión de Profesores
  Como profesor del sistema
  Quiero poder realizar todas las operaciones disponibles en mi perfil
  Para gestionar mi cuenta, revisar programas, comentar y compartir ejemplos

  Background:
    Given la API está disponible

  Scenario: Login exitoso de profesor
    When envío una solicitud POST a "/profesores/login" con el siguiente cuerpo:
      """
      {
        "email": "anrumo232@gmail.com",
        "contrasena": "1234"
      }
      """
    Then la respuesta debe tener el código 200
    And la respuesta JSON contiene el campo "token"

  Scenario: Login fallido con credenciales incorrectas
    When envío una solicitud POST a "/profesores/login" con el siguiente cuerpo:
      """
      {
        "email": "profesor@test.com",
        "contrasena": "ContraseñaIncorrecta"
      }
      """
    Then la respuesta debe tener el código 404
    And la respuesta JSON contiene el campo "error"

  Scenario: Acceso no autorizado para actualizar otro profesor
    Given estoy autenticado como profesor con email "anrumo232@gmail.com" y contraseña "1234"
    And existe otro profesor con email "juang@gmail.com", nombre "Juan Gallego", contraseña "1234"
    When envío una solicitud PUT a "/profesores/{id}" para el otro profesor con el siguiente cuerpo:
      """
      {
        "email": "hacker@test.com",
        "nombre": "Hacker"
      }
      """
    Then la respuesta debe tener el código 403
    And la respuesta JSON contiene el campo "error"

  Scenario: Obtener todos los programas de estudiantes para revisión
    Given estoy autenticado como profesor con email "anrumo232@gmail.com" y contraseña "1234"
    When envío una solicitud GET a "/profesores/programas"
    Then la respuesta debe tener el código 200
    And la respuesta es una lista de programas

  Scenario: Añadir comentario a programa de estudiante
    Given estoy autenticado como profesor con email "anrumo232@gmail.com" y contraseña "1234"
    And existe un programa con id "1"
    When envío una solicitud POST a "/profesores/programas/1/comentarios" con el siguiente cuerpo:
      """
      {
        "texto": "Excelente trabajo, pero podrías mejorar la eficiencia del algoritmo."
      }
      """
    Then la respuesta debe tener el código 201
    And la respuesta JSON contiene el campo "texto" con valor "Excelente trabajo, pero podrías mejorar la eficiencia del algoritmo."

  Scenario: Crear nuevo ejemplo de código
    Given estoy autenticado como profesor con email "anrumo232@gmail.com" y contraseña "1234"
    When envío una solicitud POST a "/profesores/ejemplos" con el siguiente cuerpo:
      """
      {
        "titulo": "Ejemplo Básico de Bucle For",
        "descripcion": "Muestra cómo usar un bucle for para iterar.",
        "codigoFuente": "public class Main { public static void main(String[] args) { for (int i = 0; i < 5; i++) { System.out.println(i); } } }",
        "tema": "Estructuras de control",
        "shared": true,
        "tags": ["bucle", "for", "fundamentos"],
        "difficulty": "BEGINNER"
      }
      """
    Then la respuesta debe tener el código 201
    And la respuesta JSON contiene el campo "titulo" con valor "Ejemplo Básico de Bucle For"
    And la respuesta JSON contiene el campo "tema" con valor "Estructuras de control"

  Scenario: Generar informe de progreso de estudiantes
    Given estoy autenticado como profesor con email "anrumo232@gmail.com" y contraseña "1234"
    When envío una solicitud GET a "/profesores/informes"
    Then la respuesta debe tener el código 200
    And la respuesta es un archivo PDF
    And el header "Content-Disposition" contiene "informe_progreso_estudiantes.pdf"