package com.uq.controller;

import com.uq.dto.EstudianteDTO;
import com.uq.dto.EstudianteLoginDTO;
import com.uq.dto.EstudianteVerificationRequest;
import com.uq.dto.ProgramaDTO;
import com.uq.dto.UserResponse;
import com.uq.exception.AccountAlreadyVerifiedException;
import com.uq.exception.ExpiredVerificationCodeException;
import com.uq.exception.InactiveAccountException;
import com.uq.exception.InvalidCredentialsException;
import com.uq.exception.InvalidVerificationCodeException;
import com.uq.exception.ProgramNotFoundException;
import com.uq.exception.UnauthorizedException;
import com.uq.exception.UserNotFoundException;
import com.uq.mapper.EstudianteMapper;
import com.uq.security.JWTUtil;
import com.uq.security.TokenResponse;
import com.uq.service.EstudianteService;
import com.uq.service.ProgramaService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


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

    private static final Logger LOGGER = Logger.getLogger(EstudianteController.class.getName());

    @Inject
    EstudianteService estudianteService;

    @Inject
    EstudianteMapper estudianteMapper;

    @Inject
    ProgramaService programaService; // Inyectar ProgramaService

    // Inyectar SecurityContext para obtener información del usuario autenticado
    @Context
    SecurityContext securityContext;


    // --- Endpoints de Registro, Verificación y Login ---

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
            estudianteService.registerEstudiante(request);
            return Response.status(Response.Status.CREATED)
                    .entity("{\"message\": \"Usuario registrado. Por favor, verifica tu correo electrónico para activar tu cuenta.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
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
            estudianteService.verifyEstudiante(request.getEmail(), request.getCode());
            return Response.ok("{\"message\": \"Cuenta verificada exitosamente.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();

        } catch (UserNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (InvalidVerificationCodeException | ExpiredVerificationCodeException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (AccountAlreadyVerifiedException e) {
            return Response.status(Response.Status.CONFLICT).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        }
        catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Error en el servidor durante la verificación.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }


    @POST
    @Path("/login")
    @Tag(name = "Login")
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
            String userEmail = estudianteService.login(request.getEmail(), request.getContrasena());
            String jwtToken = JWTUtil.generateToken(userEmail);
            return Response.ok().entity(new TokenResponse(jwtToken)).build();

        } catch (UserNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (InvalidCredentialsException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (InactiveAccountException e) {
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        }
        catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Error en el servidor durante el inicio de sesión.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // *** Endpoints de Actualización y Eliminación de Estudiante (requieren JWT Auth) ***

    @PUT
    @Path("/{id}")
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Actualiza un usuario existente (completo)", description = "Actualiza todos los campos de un usuario. Requiere autenticación.")
    @APIResponse(responseCode = "200", description = "Usuario actualizado exitosamente")
    @APIResponse(responseCode = "400", description = "Datos incompletos o formato inválido")
    @APIResponse(responseCode = "401", description = "No autenticado (Falta JWT o es inválido)")
    @APIResponse(responseCode = "403", description = "No autorizado (el usuario autenticado no coincide con el ID)")
    @APIResponse(responseCode = "404", description = "Usuario no encontrado")
    @APIResponse(responseCode = "500", description = "Error en el servidor")
    public Response updateUser(@PathParam("id") Long id, @Valid EstudianteDTO request) {
        // Lógica de autorización: verificar que el usuario autenticado es el mismo que el ID del path
        if (!isAuthorizedEstudiante(id)) {
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"No autorizado para modificar este usuario.\"}").type(MediaType.APPLICATION_JSON).build();
        }

        try {
            UserResponse updatedUser = estudianteService.updateEstudiante(id, estudianteMapper.toEntity(request));
            return Response.ok(updatedUser).build();
        } catch (UserNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor durante la actualización.\"}").type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PATCH
    @Path("/{id}")
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Actualiza un usuario existente (parcial)", description = "Actualiza campos específicos de un usuario. Requiere autenticación.")
    @APIResponse(responseCode = "200", description = "Usuario actualizado exitosamente")
    @APIResponse(responseCode = "400", description = "Datos incompletos o formato inválido")
    @APIResponse(responseCode = "401", description = "No autenticado (Falta JWT o es inválido)")
    @APIResponse(responseCode = "403", description = "No autorizado (el usuario autenticado no coincide con el ID)")
    @APIResponse(responseCode = "404", description = "Usuario no encontrado")
    @APIResponse(responseCode = "500", description = "Error en el servidor")
    public Response partialUpdateUser(@PathParam("id") Long id, EstudianteDTO request) {
        // Lógica de autorización: verificar que el usuario autenticado es el mismo que el ID del path
        if (!isAuthorizedEstudiante(id)) {
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"No autorizado para modificar este usuario.\"}").type(MediaType.APPLICATION_JSON).build();
        }

        try {
            UserResponse updatedUser = estudianteService.partialUpdateUser(id, estudianteMapper.toEntity(request));
            return Response.ok(updatedUser).build();
        } catch (UserNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor durante la actualización parcial.\"}").type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Elimina un usuario por ID", description = "Elimina un usuario del sistema. Requiere autenticación.")
    @APIResponse(responseCode = "204", description = "Usuario eliminado exitosamente")
    @APIResponse(responseCode = "401", description = "No autenticado (Falta JWT o es inválido)")
    @APIResponse(responseCode = "403", description = "No autorizado (el usuario autenticado no coincide con el ID)")
    @APIResponse(responseCode = "404", description = "Usuario no encontrado")
    @APIResponse(responseCode = "500", description = "Error en el servidor")
    public Response deleteUser(@PathParam("id") Long id) {
        // Lógica de autorización: verificar que el usuario autenticado es el mismo que el ID del path
        if (!isAuthorizedEstudiante(id)) {
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"No autorizado para eliminar este usuario.\"}").type(MediaType.APPLICATION_JSON).build();
        }

        try {
            estudianteService.deleteUser(id); // El service lanza UserNotFoundException si no existe
            return Response.noContent().build();
        } catch (UserNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor durante la eliminación.\"}").type(MediaType.APPLICATION_JSON).build();
        }
    }

    // *** Método auxiliar para verificar autorización de Estudiante ***
    // Este método obtiene el ID del usuario autenticado del SecurityContext
    // y lo compara con el ID del path.
    private boolean isAuthorizedEstudiante(Long requestedEstudianteId) {
        LOGGER.info("-> isAuthorizedEstudiante llamado con requestedEstudianteId: " + requestedEstudianteId);

        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            LOGGER.warning("-> isAuthorizedEstudiante: SecurityContext o Principal es null. Retornando false.");
            return false;
        }

        String authenticatedUserEmail = securityContext.getUserPrincipal().getName();
        LOGGER.info("-> isAuthorizedEstudiante: Email del usuario autenticado desde JWT: " + authenticatedUserEmail);


        Long authenticatedEstudianteId = null; // Inicializar a null
        try {
            // Intentar obtener el ID del usuario autenticado desde el Service
            authenticatedEstudianteId = estudianteService.getIdByEmail(authenticatedUserEmail);
            LOGGER.info("-> isAuthorizedEstudiante: ID del usuario autenticado desde DB: " + authenticatedEstudianteId);

            // Verificar si el ID del path coincide con el ID del usuario autenticado
            boolean isMatch = authenticatedEstudianteId != null && authenticatedEstudianteId.equals(requestedEstudianteId);
            LOGGER.info("-> isAuthorizedEstudiante: Comparando authenticatedEstudianteId (" + authenticatedEstudianteId + ") con requestedEstudianteId (" + requestedEstudianteId + "). Coinciden: " + isMatch);

            return isMatch; // Devuelve true si coinciden, false en caso contrario

        } catch (UserNotFoundException e) {
            LOGGER.severe("-> isAuthorizedEstudiante: ERROR: Usuario autenticado no encontrado en DB por email '" + authenticatedUserEmail + "'. Retornando false.");
            return false; // Un usuario autenticado que no existe en DB es un problema
        } catch (Exception e) {
            // Loggear cualquier otro error inesperado que ocurra al obtener el ID
            LOGGER.log(Level.SEVERE, "-> isAuthorizedEstudiante: ERROR inesperado al obtener ID del usuario autenticado.", e);
            return false;
        }
    }


    // ******************************************************
    // --- Aquí empieza la lógica de gestión de Programas ---
    // ******************************************************

    // Endpoint para crear un nuevo programa para un estudiante específico
    @POST
    @Path("/{estudianteId}/programas")
    @SecurityRequirement(name = "jwtAuth") // Requiere autenticación con JWT
    @Operation(summary = "Crea un nuevo programa para un estudiante", description = "Asocia y guarda un nuevo programa para el estudiante especificado. Requiere autenticación.")
    @APIResponse(responseCode = "201", description = "Programa creado exitosamente",
            content = @Content(schema = @Schema(implementation = ProgramaDTO.class)))
    @APIResponse(responseCode = "400", description = "Datos del programa incompletos o formato inválido")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "403", description = "No autorizado (el usuario autenticado no coincide con estudianteId)")
    @APIResponse(responseCode = "404", description = "Estudiante no encontrado")
    @APIResponse(responseCode = "500", description = "Error en el servidor")
    public Response createProgramaForEstudiante(
            @PathParam("estudianteId") Long estudianteId,
            @Valid ProgramaDTO programaDTO
    ) {
        // Lógica de autorización: verificar que el usuario autenticado es el mismo que el estudianteId del path
        if (!isAuthorizedEstudiante(estudianteId)) {
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"No autorizado para crear programas para este usuario.\"}").type(MediaType.APPLICATION_JSON).build();
        }

        try {
            ProgramaDTO createdPrograma = programaService.createProgram(estudianteId, programaDTO);
            return Response.status(Response.Status.CREATED).entity(createdPrograma).build();
        } catch (UserNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al crear programa.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // Endpoint para listar todos los programas de un estudiante específico
    @GET
    @Path("/{estudianteId}/programas")
    @SecurityRequirement(name = "jwtAuth") // Requiere autenticación con JWT
    @Operation(summary = "Obtiene todos los programas de un estudiante", description = "Lista todos los programas asociados al estudiante especificado. Requiere autenticación.")
    @APIResponse(responseCode = "200", description = "Lista de programas obtenida exitosamente",
            content = @Content(schema = @Schema(implementation = ProgramaDTO.class)))
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "403", description = "No autorizado (el usuario autenticado no coincide con estudianteId)")
    @APIResponse(responseCode = "404", description = "Estudiante no encontrado")
    @APIResponse(responseCode = "500", description = "Error en el servidor")
    public Response getProgramasByEstudianteId(
            @PathParam("estudianteId") Long estudianteId
    ) {
        // Lógica de autorización: verificar que el usuario autenticado es el mismo que el estudianteId del path
        if (!isAuthorizedEstudiante(estudianteId)) {
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"No autorizado para ver programas de este usuario.\"}").type(MediaType.APPLICATION_JSON).build();
        }

        try {
            List<ProgramaDTO> programas = programaService.getProgramsByEstudianteId(estudianteId);
            return Response.ok(programas).build();
        } catch (UserNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al obtener programas.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // Endpoint para obtener los detalles de un programa específico por su ID
    @GET
    @Path("/programas/{programaId}") // Este path no está bajo /estudiantes/{id} porque el ID del programa es único globalmente
    @SecurityRequirement(name = "jwtAuth") // Requiere autenticación con JWT
    @Operation(summary = "Obtiene los detalles de un programa por ID", description = "Retorna la información de un programa específico. Requiere autenticación y ser el dueño del programa.")
    @APIResponse(responseCode = "200", description = "Programa encontrado exitosamente",
            content = @Content(schema = @Schema(implementation = ProgramaDTO.class)))
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "403", description = "No autorizado (el usuario autenticado no es el dueño)")
    @APIResponse(responseCode = "404", description = "Programa no encontrado")
    @APIResponse(responseCode = "500", description = "Error en el servidor")
    public Response getProgramaById(
            @PathParam("programaId") Long programaId
    ) {
        // Obtener el ID del usuario autenticado para pasarlo al service para verificación de propiedad
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String authenticatedUserEmail = securityContext.getUserPrincipal().getName();
        Long authenticatedEstudianteId;
        try {
            authenticatedEstudianteId = estudianteService.getIdByEmail(authenticatedUserEmail);
        } catch (UserNotFoundException e) {
            // Usuario autenticado no encontrado en DB - problema grave o token inválido/obsoleto
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error al obtener información del usuario autenticado.\"}").type(MediaType.APPLICATION_JSON).build();
        }


        try {
            // Llama al service pasando el ID del programa y el ID del usuario autenticado
            ProgramaDTO programa = programaService.getProgramById(programaId, authenticatedEstudianteId);

            return Response.ok(programa).build();
        } catch (ProgramNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (UnauthorizedException e) { // Capturar la nueva excepción específica
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        }
        catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al obtener programa.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }
}