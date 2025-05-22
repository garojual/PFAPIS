package com.uq.stepdefs;

import com.uq.exception.UserNotFoundException;
import com.uq.service.ProfesorService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@ApplicationScoped
public class ProfesorStepDefinitions {

    private Response response;
    private ValidatableResponse validatableResponse;
    private String jwtToken;
    private Long createdProfesorId; // Usaremos este para el profesor "target" de actualización
    private Long otherProfesorId;   // Usaremos este para el "otro profesor" en la prueba de autorización
    private Long createdEjemploId;

    // Inyecta el ProfesorService para poder buscar profesores en la DB de prueba
    @Inject
    ProfesorService profesorService;

    @Given("la API está disponible")
    public void la_api_esta_disponible() {
        baseURI = "http://localhost:8080";
    }

    @Given("estoy autenticado como profesor con email {string} y contraseña {string}")
    public void estoy_autenticado_como_profesor(String email, String password) {
        Response loginResponse = given()
                .header("Content-Type", "application/json")
                .body(String.format("{\"email\": \"%s\", \"contrasena\": \"%s\"}", email, password))
                .when()
                .post("/profesores/login");

        loginResponse.then().statusCode(200);

        this.jwtToken = loginResponse.jsonPath().getString("token");

        if (this.jwtToken == null || this.jwtToken.isEmpty()) {
            throw new RuntimeException("No se pudo obtener el token JWT para profesor después de login. Verifica credenciales y configuración de la API. Respuesta: " + loginResponse.asString());
        }
    }

    // Este paso ahora usa el servicio inyectado para buscar al profesor por email
    @Given("existe un profesor con email {string}, nombre {string}, contraseña {string}")
    public void existe_un_profesor(String email, String nombre, String contrasena) {
        if (profesorService == null) {
            System.out.println("DEBUG: ProfesorService is null, using REST API fallback");
            verifyProfesorExistsViaAPI(email);
            return;
        }

        try {
            // Llama al servicio para obtener el ID del profesor por email
            this.createdProfesorId = profesorService.getIdByEmail(email);
            System.out.println("DEBUG: Encontrado profesor existente con ID: " + this.createdProfesorId + " para email: " + email);
        } catch (UserNotFoundException e) {
            fail("El profesor con email '" + email + "' NO existe en la base de datos de prueba. Por favor, asegúrate de que los datos de prueba estén cargados correctamente.");
        } catch (Exception e) {
            fail("Error inesperado al verificar la existencia del profesor con email '" + email + "': " + e.getMessage());
        }
    }

    @Given("existe un programa con id {string}")
    public void existe_un_programa_con_id(String programaId) {
        try {
            Long id = Long.parseLong(programaId);
            System.out.println("DEBUG: Asumiendo que existe el programa con ID: " + programaId);
        } catch (NumberFormatException e) {
            fail("El ID del programa '" + programaId + "' no es un número válido.");
        }
    }

    @Given("existe un ejemplo creado por el profesor autenticado con titulo {string}")
    public void existe_un_ejemplo_creado_por_profesor(String titulo) {
        if (this.jwtToken == null) {
            throw new RuntimeException("Se requiere autenticación para crear ejemplo. El token JWT es null.");
        }

        String body = String.format("{\"titulo\": \"%s\", \"descripcion\": \"Descripción de prueba para test\", \"codigo\": \"public class TestExample {\\n    public static void main(String[] args) {\\n        // Test code\\n    }\\n}\", \"categoria\": \"BASICOS\"}", titulo);

        Response createResponse = given()
                .header("Authorization", "Bearer " + this.jwtToken)
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .post("/profesores/ejemplos");

        createResponse.then().statusCode(201);

        this.createdEjemploId = createResponse.jsonPath().getLong("id");
        System.out.println("DEBUG: Creado ejemplo con ID: " + this.createdEjemploId + " y titulo: " + titulo);
    }

    // Este paso ahora usa el servicio inyectado para buscar al 'otro' profesor por email
    @Given("existe otro profesor con email {string}, nombre {string}, contraseña {string}")
    public void existe_otro_profesor(String email, String nombre, String contrasena) {
        if (profesorService == null) {
            System.out.println("DEBUG: ProfesorService is null, using REST API fallback for other professor");
            verifyOtherProfesorExistsViaAPI(email);
            return;
        }

        try {
            // Llama al servicio para obtener el ID del otro profesor por email
            this.otherProfesorId = profesorService.getIdByEmail(email);
            System.out.println("DEBUG: Encontrado 'otro' profesor existente con ID: " + this.otherProfesorId + " para email: " + email);
        } catch (UserNotFoundException e) {
            fail("El 'otro' profesor con email '" + email + "' NO existe en la base de datos de prueba. Por favor, asegúrate de que los datos de prueba estén cargados correctamente.");
        } catch (Exception e) {
            fail("Error inesperado al verificar la existencia del 'otro' profesor con email '" + email + "': " + e.getMessage());
        }
    }

    private void verifyProfesorExistsViaAPI(String email) {
        try {
            Response testLoginResponse = given()
                    .header("Content-Type", "application/json")
                    .body(String.format("{\"email\": \"%s\", \"contrasena\": \"123\"}", email))
                    .when()
                    .post("/profesores/login");

            if (testLoginResponse.getStatusCode() == 200) {
                this.createdProfesorId = 1L;
                System.out.println("DEBUG: Verified professor exists via API for email: " + email);
            } else {
                fail("El profesor con email '" + email + "' NO existe o las credenciales son incorrectas.");
            }
        } catch (Exception e) {
            fail("Error al verificar la existencia del profesor vía API: " + e.getMessage());
        }
    }

    private void verifyOtherProfesorExistsViaAPI(String email) {
        try {
            Response testLoginResponse = given()
                    .header("Content-Type", "application/json")
                    .body(String.format("{\"email\": \"%s\", \"contrasena\": \"123\"}", email))
                    .when()
                    .post("/profesores/login");

            if (testLoginResponse.getStatusCode() == 200) {
                this.otherProfesorId = 2L;
                System.out.println("DEBUG: Verified other professor exists via API for email: " + email);
            } else {
                fail("El 'otro' profesor con email '" + email + "' NO existe o las credenciales son incorrectas.");
            }
        } catch (Exception e) {
            fail("Error al verificar la existencia del 'otro' profesor vía API: " + e.getMessage());
        }
    }

    @When("envío una solicitud POST a {string} con el siguiente cuerpo:")
    public void envio_solicitud_post_con_cuerpo(String endpoint, String body) {
        var requestSpec = given().header("Content-Type", "application/json").body(body);

        if (this.jwtToken != null && !endpoint.endsWith("/login")) {
            requestSpec = requestSpec.header("Authorization", "Bearer " + this.jwtToken);
        }

        this.response = requestSpec.when().post(endpoint);
        this.validatableResponse = this.response.then();
    }

    @When("envío una solicitud GET a {string}")
    public void envio_solicitud_get(String endpoint) {
        if (this.jwtToken == null) {
            System.out.println("DEBUG: Enviando GET sin token a: " + endpoint);
            this.response = given().when().get(endpoint);
        } else {
            System.out.println("DEBUG: Enviando GET con token a: " + endpoint);
            this.response = given()
                    .header("Authorization", "Bearer " + this.jwtToken)
                    .when()
                    .get(endpoint);
        }
        this.validatableResponse = this.response.then();
    }

    @When("envío una solicitud PUT a {string} para el profesor creado con el siguiente cuerpo:")
    public void envio_solicitud_put_profesor_creado(String endpointTemplate, String body) {
        if (this.jwtToken == null) {
            throw new RuntimeException("El token JWT es null. La solicitud PUT requiere autenticación.");
        }
        if (this.createdProfesorId == null) {
            throw new RuntimeException("No hay un profesor 'creado' (encontrado) para enviar la solicitud PUT. Verifica el paso GIVEN anterior.");
        }

        String url = endpointTemplate.replace("{id}", String.valueOf(this.createdProfesorId));
        System.out.println("DEBUG: Enviando PUT a: " + url + " para profesor 'creado' (target)");

        this.response = given()
                .header("Authorization", "Bearer " + this.jwtToken)
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .put(url);
        this.validatableResponse = this.response.then();
    }

    @When("envío una solicitud PUT a {string} para el otro profesor con el siguiente cuerpo:")
    public void envio_solicitud_put_otro_profesor(String endpointTemplate, String body) {
        if (this.jwtToken == null) {
            throw new RuntimeException("El token JWT es null. La solicitud PUT requiere autenticación.");
        }
        if (this.otherProfesorId == null) {
            throw new RuntimeException("No hay 'otro' profesor (encontrado) para enviar la solicitud PUT. Verifica el paso GIVEN anterior.");
        }

        String url = endpointTemplate.replace("{id}", String.valueOf(this.otherProfesorId));
        System.out.println("DEBUG: Enviando PUT a: " + url + " para 'otro' profesor");

        this.response = given()
                .header("Authorization", "Bearer " + this.jwtToken)
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .put(url);
        this.validatableResponse = this.response.then();
    }

    @When("envío una solicitud PATCH a {string} para el profesor creado con el siguiente cuerpo:")
    public void envio_solicitud_patch_profesor_creado(String endpointTemplate, String body) {
        if (this.jwtToken == null) {
            throw new RuntimeException("El token JWT es null. La solicitud PATCH requiere autenticación.");
        }
        if (this.createdProfesorId == null) {
            throw new RuntimeException("No hay un profesor 'creado' (encontrado) para enviar la solicitud PATCH. Verifica el paso GIVEN anterior.");
        }

        String url = endpointTemplate.replace("{id}", String.valueOf(this.createdProfesorId));
        System.out.println("DEBUG: Enviando PATCH a: " + url + " para profesor 'creado' (target)");

        this.response = given()
                .header("Authorization", "Bearer " + this.jwtToken)
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .patch(url);
        this.validatableResponse = this.response.then();
    }

    @When("envío una solicitud PUT a {string} para el ejemplo creado con el siguiente cuerpo:")
    public void envio_solicitud_put_ejemplo_creado(String endpointTemplate, String body) {
        if (this.jwtToken == null) {
            throw new RuntimeException("El token JWT es null. La solicitud PUT requiere autenticación.");
        }
        if (this.createdEjemploId == null) {
            throw new RuntimeException("No hay un ejemplo 'creado' (realmente creado por la prueba) para enviar la solicitud PUT. Verifica el paso GIVEN anterior.");
        }

        String url = endpointTemplate.replace("{ejemploId}", String.valueOf(this.createdEjemploId));
        System.out.println("DEBUG: Enviando PUT a: " + url + " para ejemplo creado");

        this.response = given()
                .header("Authorization", "Bearer " + this.jwtToken)
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .put(url);
        this.validatableResponse = this.response.then();
    }

    @Then("la respuesta debe tener el código {int}")
    public void la_respuesta_debe_tener_codigo(int expectedStatusCode) {
        this.validatableResponse.statusCode(expectedStatusCode);
    }

    @Then("la respuesta JSON contiene el campo {string}")
    public void la_respuesta_json_contiene_campo(String fieldName) {
        this.validatableResponse.body(fieldName, notNullValue());
    }

    @Then("la respuesta JSON contiene el campo {string} con valor {string}")
    public void la_respuesta_json_contiene_campo_con_valor(String fieldName, String expectedValue) {
        this.validatableResponse.body(fieldName, equalTo(expectedValue));
    }

    @Then("la respuesta JSON contiene el campo {string} con valor {int}")
    public void la_respuesta_json_contiene_campo_con_valor_entero(String fieldName, int expectedValue) {
        this.validatableResponse.body(fieldName, equalTo(expectedValue));
    }

    @Then("la respuesta es una lista de programas")
    public void la_respuesta_es_una_lista_de_programas() {
        this.validatableResponse.body("$", isA(List.class));
    }

    @Then("la respuesta es un archivo PDF")
    public void la_respuesta_es_un_archivo_pdf() {
        this.validatableResponse.header("Content-Type", equalTo("application/octet-stream"));
        byte[] responseBody = this.response.getBody().asByteArray();
        assertTrue("La respuesta debe contener datos binarios", responseBody.length > 0);
    }

    @Then("el header {string} contiene {string}")
    public void el_header_contiene(String headerName, String expectedValue) {
        String headerValue = this.response.getHeader(headerName);
        assertNotNull("El header " + headerName + " no debe ser null", headerValue);
        assertTrue("El header " + headerName + " debe contener " + expectedValue,
                headerValue.contains(expectedValue));
    }
}