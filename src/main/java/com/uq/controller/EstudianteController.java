package com.uq.controller;

import com.uq.dto.EstudianteDTO;
import com.uq.dto.EstudianteLoginDTO;
import com.uq.dto.EstudianteVerificationRequest;
import com.uq.dto.UserResponse;
import com.uq.exception.AccountAlreadyVerifiedException;
import com.uq.exception.ExpiredVerificationCodeException;
import com.uq.exception.InactiveAccountException;
import com.uq.exception.InvalidCredentialsException;
import com.uq.exception.InvalidVerificationCodeException;
import com.uq.exception.UserNotFoundException;
import com.uq.mapper.EstudianteMapper;
import com.uq.security.JWTUtil;
import com.uq.security.TokenResponse;
import com.uq.service.EstudianteService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

// Definición del esquema de seguridad JWT para OpenAPI
@SecurityScheme(
        securitySchemeName = "jwtAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
@Path("/estudiantes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Gestión de Estudiantes", description = "API para gestionar usuarios, incluyendo registro, login, actualización completa y parcial, eliminación e inicio de sesión para seguridad.")
public class EstudianteController {

    @Inject
    EstudianteService estudianteService;

    @Inject
    EstudianteMapper estudianteMapper;

    @POST
    @Operation(summary = "Registra un nuevo estudiante", description = "Crea un nuevo estudiante en el sistema y envía un código de verificación por correo.")
    @APIResponse(responseCode = "201", description = "Usuario registrado exitosamente. Se ha enviado un correo de verificación.")
    @APIResponse(responseCode = "400", description = "Datos incompletos, formato inválido o correo/nombre de usuario ya registrado.")
    @APIResponse(responseCode = "500", description = "Error en el servidor.")
    public Response registerUser(
            @Valid @RequestBody(
                    description = "Datos del usuario a registrar",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EstudianteDTO.class),
                            examples = @ExampleObject(
                                    name = "Ejemplo de Registro",
                                    summary = "Ejemplo de entrada para registrar un usuario",
                                    description = "Este es un ejemplo de cómo enviar los datos para registrar un usuario.",
                                    value = """
                                        {
                                            "nombre": "usuarioEjemplo",
                                            "email": "usuario@example.com",
                                            "contrasena": "Password123"
                                        }
                                        """
                            )
                    )
            ) EstudianteDTO request) {
        try {
            // El service ahora maneja la creación, marcación como inactivo,
            // generación de código y (conceptualmente) el envío del email.
            // Puede que el service retorne un DTO del usuario (inactivo)
            // o simplemente confirme la operación.
            UserResponse registeredUser = estudianteService.registerEstudiante(request);

            // Responde indicando éxito y la necesidad de verificar el email.
            return Response.status(Response.Status.CREATED)
                    .entity("{\"message\": \"Usuario registrado. Por favor, verifica tu correo electrónico para activar tu cuenta.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            // Catch genérico para otros errores del servidor
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Error en el servidor durante el registro.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @POST
    @Path("/verificar")
    @Operation(summary = "Verifica la cuenta del estudiante", description = "Activa la cuenta de un estudiante usando el código de verificación enviado por email.")
    @APIResponse(responseCode = "200", description = "Cuenta verificada exitosamente.")
    @APIResponse(responseCode = "400", description = "Datos inválidos o código de verificación incorrecto/expirado.")
    @APIResponse(responseCode = "404", description = "Usuario no encontrado.")
    @APIResponse(responseCode = "409", description = "La cuenta ya ha sido verificada previamente.")
    @APIResponse(responseCode = "500", description = "Error en el servidor.")
    public Response verifyUser(@Valid EstudianteVerificationRequest request) {
        try {
            // El service verifica el código, la expiración y activa la cuenta.
            estudianteService.verifyEstudiante(request.getEmail(), request.getCode());

            return Response.ok("{\"message\": \"Cuenta verificada exitosamente.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();

        } catch (UserNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (InvalidVerificationCodeException | ExpiredVerificationCodeException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (AccountAlreadyVerifiedException e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        }
        catch (Exception e) {
            // Catch genérico para otros errores del servidor
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Error en el servidor durante la verificación.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }


    @POST
    @Path("/login")
    @Tag(name = "Login") // Puedes mantener esta etiqueta o fusionarla si quieres
    @Operation(summary = "Iniciar sesión", description = "Inicia sesión, verifica credenciales y estado de activación. Genera el token para acceder a los demás endpoints.")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "200", description = "Inicio de sesión exitoso. Token generado.")
    @APIResponse(responseCode = "401", description = "Credenciales inválidas.")
    @APIResponse(responseCode = "403", description = "Cuenta inactiva. Por favor, verifica tu correo.")
    @APIResponse(responseCode = "404", description = "Usuario no encontrado.")
    @APIResponse(responseCode = "500", description = "Error en el servidor.")
    public Response login(@Valid EstudianteLoginDTO request) {
        if (request == null || request.getEmail() == null || request.getContrasena() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Solicitud inválida, faltan datos\"}").type(MediaType.APPLICATION_JSON).build();
        }

        try {
            // El service verifica credenciales Y ESTADO DE ACTIVACIÓN.
            // Si las credenciales son correctas y la cuenta está activa, devuelve algo útil
            // como el email o ID del usuario para generar el token.
            String userEmail = estudianteService.login(request.getEmail(), request.getContrasena());

            // Si el service no lanzó excepción, el login fue exitoso y la cuenta está activa.
            // Generar el token JWT.
            String jwtToken = JWTUtil.generateToken(userEmail);

            // Devolver el token en la respuesta
            return Response.ok().entity(new TokenResponse(jwtToken)).build();

        } catch (UserNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"Usuario no encontrado.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (InvalidCredentialsException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\": \"Credenciales inválidas.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (InactiveAccountException e) {
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"Cuenta inactiva. Por favor, verifica tu correo electrónico.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }
        catch (Exception e) {
            // Catch genérico para otros errores del servidor
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Error en el servidor durante el inicio de sesión.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    // *** Endpoints de Actualización y Eliminación (requieren JWT Auth) ***

    @PUT
    @Path("/{id}")
    @SecurityRequirement(name = "jwtAuth") // Requiere autenticación con JWT
    @Operation(summary = "Actualiza un usuario existente (completo)", description = "Actualiza todos los campos de un usuario. Requiere autenticación.")
    @APIResponse(responseCode = "200", description = "Usuario actualizado exitosamente")
    @APIResponse(responseCode = "400", description = "Datos incompletos o formato inválido")
    @APIResponse(responseCode = "401", description = "No autenticado (Falta JWT o es inválido)")
    @APIResponse(responseCode = "403", description = "No autorizado (JWT no tiene permisos o no coincide con el usuario del ID)") // Considerar lógica de autorización
    @APIResponse(responseCode = "404", description = "Usuario no encontrado")
    @APIResponse(responseCode = "500", description = "Error en el servidor")
    public Response updateUser(@PathParam("id") Long id, @Valid EstudianteDTO request) {
        try {
            UserResponse updatedUser = estudianteService.updateEstudiante(id, estudianteMapper.toEntity(request));
            if (updatedUser == null) {
                // Si el service retorna null para NOT_FOUND
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"Usuario no encontrado\"}").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.ok(updatedUser).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor durante la actualización\"}").type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PATCH
    @Path("/{id}")
    @SecurityRequirement(name = "jwtAuth") // Requiere autenticación con JWT
    @Operation(summary = "Actualiza un usuario existente (parcial)", description = "Actualiza campos específicos de un usuario. Requiere autenticación.")
    @APIResponse(responseCode = "200", description = "Usuario actualizado exitosamente")
    @APIResponse(responseCode = "400", description = "Datos incompletos o formato inválido")
    @APIResponse(responseCode = "401", description = "No autenticado (Falta JWT o es inválido)")
    @APIResponse(responseCode = "403", description = "No autorizado (JWT no tiene permisos o no coincide con el usuario del ID)") // Considerar lógica de autorización
    @APIResponse(responseCode = "404", description = "Usuario no encontrado")
    @APIResponse(responseCode = "500", description = "Error en el servidor")
    public Response partialUpdateUser(@PathParam("id") Long id, EstudianteDTO request) {
        try {
            UserResponse updatedUser = estudianteService.partialUpdateUser(id, estudianteMapper.toEntity(request));
            if (updatedUser == null) {
                // Si el service retorna null para NOT_FOUND
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"Usuario no encontrado\"}").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.ok(updatedUser).build();
        } catch (IllegalArgumentException e) {
            // Manejo específico si el servicio usa IllegalArgumentException para NOT_FOUND
            if (e.getMessage().contains("Usuario no encontrado")) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor durante la actualización parcial\"}").type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @SecurityRequirement(name = "jwtAuth") // Requiere autenticación con JWT
    @Operation(summary = "Elimina un usuario por ID", description = "Elimina un usuario del sistema. Requiere autenticación.")
    @APIResponse(responseCode = "204", description = "Usuario eliminado exitosamente")
    @APIResponse(responseCode = "401", description = "No autenticado (Falta JWT o es inválido)")
    @APIResponse(responseCode = "403", description = "No autorizado (JWT no tiene permisos o no coincide con el usuario del ID)") // Considerar lógica de autorización
    @APIResponse(responseCode = "404", description = "Usuario no encontrado")
    @APIResponse(responseCode = "500", description = "Error en el servidor")
    public Response deleteUser(@PathParam("id") Long id) {
        try {
            boolean deleted = estudianteService.deleteUser(id);
            if (!deleted) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"Usuario no encontrado\"}").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.noContent().build(); // 204 No Content es estándar para eliminación exitosa
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor durante la eliminación\"}").type(MediaType.APPLICATION_JSON).build();
        }
    }
}