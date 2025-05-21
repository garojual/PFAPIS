package com.uq.controller;

import com.uq.dto.*;
import com.uq.exception.*;
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
    ProgramaService programaService;

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
        if (!isAuthorizedEstudiante(id)) {
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"No autorizado para eliminar este usuario.\"}").type(MediaType.APPLICATION_JSON).build();
        }
        try {
            estudianteService.deleteUser(id);
            return Response.noContent().build();
        } catch (UserNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor durante la eliminación.\"}").type(MediaType.APPLICATION_JSON).build();
        }
    }

    // *** Método auxiliar para verificar autorización de Estudiante (para endpoints /estudiantes/{id}) ***
    private boolean isAuthorizedEstudiante(Long requestedEstudianteId) {
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            LOGGER.warning("-> isAuthorizedEstudiante: SecurityContext o Principal es null. Retornando false.");
            return false;
        }
        String authenticatedUserEmail = securityContext.getUserPrincipal().getName();
        try {
            Long authenticatedEstudianteId = estudianteService.getIdByEmail(authenticatedUserEmail);
            boolean isMatch = requestedEstudianteId != null && authenticatedEstudianteId != null && authenticatedEstudianteId.equals(requestedEstudianteId);
            return isMatch;
        } catch (UserNotFoundException e) {
            LOGGER.log(Level.SEVERE, "-> isAuthorizedEstudiante: ERROR: Usuario autenticado con email '" + authenticatedUserEmail + "' no encontrado en DB.", e);
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "-> isAuthorizedEstudiante: ERROR inesperado al obtener ID del usuario autenticado por email.", e);
            return false;
        }
    }

    // Endpoint para obtener los programas del usuario autenticado actual
    @GET
    @Path("/me/programas")
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Obtiene todos los programas del usuario autenticado", description = "Lista todos los programas del usuario que ha iniciado sesión.")
    @APIResponse(responseCode = "200", description = "Lista de programas obtenida exitosamente",
            content = @Content(schema = @Schema(implementation = ProgramaDTO.class)))
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "404", description = "Usuario autenticado no encontrado en DB (Error interno o configuración)")
    @APIResponse(responseCode = "500", description = "Error en el servidor")
    public Response getMyProgramas() {
        // Obtener el ID del usuario autenticado del SecurityContext
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            LOGGER.severe("-> getMyProgramas: Endpoint protegido pero SecurityContext/Principal es null.");
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String authenticatedUserEmail = securityContext.getUserPrincipal().getName();
        Long authenticatedEstudianteId;
        try {
            authenticatedEstudianteId = estudianteService.getIdByEmail(authenticatedUserEmail);
            if (authenticatedEstudianteId == null) {
                LOGGER.severe("-> getMyProgramas: Usuario autenticado con email '" + authenticatedUserEmail + "' no encontrado en DB después de autenticación JWT exitosa.");
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"Usuario autenticado no encontrado.\"}").type(MediaType.APPLICATION_JSON).build();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "-> getMyProgramas: ERROR inesperado al obtener ID del usuario autenticado.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al obtener información del usuario autenticado.\"}").type(MediaType.APPLICATION_JSON).build();
        }

        try {
            // Llama al servicio para obtener los programas usando el ID del usuario autenticado
            List<ProgramaDTO> programas = programaService.getProgramsByEstudianteId(authenticatedEstudianteId);
            return Response.ok(programas).build();
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inesperado al obtener programas del usuario autenticado.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al obtener tus programas.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // ******************************************************
    // --- Aquí empieza la lógica de gestión de Programas ---
    // ******************************************************

    // Endpoint para crear un nuevo programa para un estudiante específico
    @POST
    @Path("/{estudianteId}/programas")
    @SecurityRequirement(name = "jwtAuth")
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
            LOGGER.log(Level.SEVERE, "Error inesperado al crear programa.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al crear programa.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // Endpoint para listar todos los programas de un estudiante específico
    @GET
    @Path("/{estudianteId}/programas")
    @SecurityRequirement(name = "jwtAuth")
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
        if (!isAuthorizedEstudiante(estudianteId)) {
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"No autorizado para ver programas de este usuario.\"}").type(MediaType.APPLICATION_JSON).build();
        }
        try {
            List<ProgramaDTO> programas = programaService.getProgramsByEstudianteId(estudianteId);
            return Response.ok(programas).build();
        } catch (UserNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inesperado al obtener programas.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al obtener programas.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // Endpoint para obtener los detalles de un programa específico por su ID
    @GET
    @Path("/programas/{programaId}")
    @SecurityRequirement(name = "jwtAuth")
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
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            LOGGER.severe("-> getProgramaById: Endpoint protegido pero SecurityContext/Principal es null.");
            return Response.status(Response.Status.UNAUTHORIZED).build(); // Debería ser 401 por @Authenticated
        }
        String authenticatedUserEmail = securityContext.getUserPrincipal().getName();
        Long authenticatedEstudianteId;
        try {
            authenticatedEstudianteId = estudianteService.getIdByEmail(authenticatedUserEmail);
            if (authenticatedEstudianteId == null) {
                LOGGER.severe("-> getProgramaById: Usuario autenticado con email '" + authenticatedUserEmail + "' no encontrado en DB después de autenticación JWT exitosa.");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error interno: Usuario autenticado no encontrado.\"}").type(MediaType.APPLICATION_JSON).build();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "-> getProgramaById: ERROR inesperado al obtener ID del usuario autenticado.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al obtener información del usuario autenticado.\"}").type(MediaType.APPLICATION_JSON).build();
        }

        try {
            // Llama al service pasando el ID del programa y el ID del usuario autenticado para verificación de propiedad
            ProgramaDTO programa = programaService.getProgramById(programaId, authenticatedEstudianteId);
            return Response.ok(programa).build();
        } catch (ProgramNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (UnauthorizedException e) { // Capturar la excepción de autorización del Service
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inesperado al obtener programa por ID.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al obtener programa.\"}").type(MediaType.APPLICATION_JSON).build();
        }
    }

    // Endpoint para actualizar un programa completo por su ID
    @PUT
    @Path("/programas/{programaId}")
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Actualiza un programa existente (completo)", description = "Actualiza todos los campos de un programa. Requiere autenticación y ser el dueño.")
    @APIResponse(responseCode = "200", description = "Programa actualizado exitosamente",
            content = @Content(schema = @Schema(implementation = ProgramaDTO.class)))
    @APIResponse(responseCode = "400", description = "Datos del programa incompletos o formato inválido")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "403", description = "No autorizado (el usuario autenticado no es el dueño)")
    @APIResponse(responseCode = "404", description = "Programa no encontrado")
    @APIResponse(responseCode = "500", description = "Error en el servidor")
    public Response updatePrograma(
            @PathParam("programaId") Long programaId,
            @Valid ProgramaDTO programaDTO // DTO con los datos de actualización
    ) {
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            LOGGER.severe("-> updatePrograma: Endpoint protegido pero SecurityContext/Principal es null.");
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String authenticatedUserEmail = securityContext.getUserPrincipal().getName();
        Long authenticatedEstudianteId;
        try {
            authenticatedEstudianteId = estudianteService.getIdByEmail(authenticatedUserEmail);
            if (authenticatedEstudianteId == null) {
                LOGGER.severe("-> updatePrograma: Usuario autenticado con email '" + authenticatedUserEmail + "' no encontrado en DB.");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error interno: Usuario autenticado no encontrado.\"}").type(MediaType.APPLICATION_JSON).build();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "-> updatePrograma: ERROR inesperado al obtener ID del usuario autenticado.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al obtener información del usuario autenticado.\"}").type(MediaType.APPLICATION_JSON).build();
        }

        try {
            ProgramaDTO updatedPrograma = programaService.updateProgram(programaId, programaDTO, authenticatedEstudianteId);
            return Response.ok(updatedPrograma).build();
        } catch (ProgramNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (UnauthorizedException e) {
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (IllegalArgumentException e) { // Para validaciones del DTO o service
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inesperado al actualizar programa.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al actualizar programa.\"}").type(MediaType.APPLICATION_JSON).build();
        }
    }

    // Endpoint para actualizar un programa parcialmente por su ID
    @PATCH
    @Path("/programas/{programaId}")
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Actualiza un programa existente (parcial)", description = "Actualiza campos específicos de un programa. Requiere autenticación y ser el dueño.")
    @APIResponse(responseCode = "200", description = "Programa actualizado exitosamente",
            content = @Content(schema = @Schema(implementation = ProgramaDTO.class)))
    @APIResponse(responseCode = "400", description = "Datos del programa incompletos o formato inválido")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "403", description = "No autorizado (el usuario autenticado no es el dueño)")
    @APIResponse(responseCode = "404", description = "Programa no encontrado")
    @APIResponse(responseCode = "500", description = "Error en el servidor")
    public Response partialUpdatePrograma(
            @PathParam("programaId") Long programaId,
            ProgramaDTO programaDTO // DTO con los datos de actualización parcial (campos no nulos)
            // Nota: @Valid no se usa típicamente para PATCH ya que los campos pueden ser nulos
    ) {
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            LOGGER.severe("-> partialUpdatePrograma: Endpoint protegido pero SecurityContext/Principal es null.");
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String authenticatedUserEmail = securityContext.getUserPrincipal().getName();
        Long authenticatedEstudianteId;
        try {
            authenticatedEstudianteId = estudianteService.getIdByEmail(authenticatedUserEmail);
            if (authenticatedEstudianteId == null) {
                LOGGER.severe("-> partialUpdatePrograma: Usuario autenticado con email '" + authenticatedUserEmail + "' no encontrado en DB.");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error interno: Usuario autenticado no encontrado.\"}").type(MediaType.APPLICATION_JSON).build();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "-> partialUpdatePrograma: ERROR inesperado al obtener ID del usuario autenticado.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al obtener información del usuario autenticado.\"}").type(MediaType.APPLICATION_JSON).build();
        }

        try {
            ProgramaDTO updatedPrograma = programaService.partialUpdateProgram(programaId, programaDTO, authenticatedEstudianteId);
            return Response.ok(updatedPrograma).build();
        } catch (ProgramNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (UnauthorizedException e) {
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (IllegalArgumentException e) { // Para validaciones del DTO o service
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inesperado al actualizar programa parcialmente.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al actualizar programa parcialmente.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }


    // Endpoint para eliminar un programa por su ID
    @DELETE
    @Path("/programas/{programaId}")
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Elimina un programa por ID", description = "Elimina un programa del sistema. Requiere autenticación y ser el dueño.")
    @APIResponse(responseCode = "204", description = "Programa eliminado exitosamente")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "403", description = "No autorizado (el usuario autenticado no es el dueño)")
    @APIResponse(responseCode = "404", description = "Programa no encontrado")
    @APIResponse(responseCode = "500", description = "Error en el servidor")
    public Response deletePrograma(
            @PathParam("programaId") Long programaId
    ) {
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            LOGGER.severe("-> deletePrograma: Endpoint protegido pero SecurityContext/Principal es null.");
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String authenticatedUserEmail = securityContext.getUserPrincipal().getName();
        Long authenticatedEstudianteId;
        try {
            authenticatedEstudianteId = estudianteService.getIdByEmail(authenticatedUserEmail);
            if (authenticatedEstudianteId == null) {
                LOGGER.severe("-> deletePrograma: Usuario autenticado con email '" + authenticatedUserEmail + "' no encontrado en DB.");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error interno: Usuario autenticado no encontrado.\"}").type(MediaType.APPLICATION_JSON).build();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "-> deletePrograma: ERROR inesperado al obtener ID del usuario autenticado.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al obtener información del usuario autenticado.\"}").type(MediaType.APPLICATION_JSON).build();
        }

        try {
            programaService.deleteProgram(programaId, authenticatedEstudianteId);
            return Response.noContent().build(); // 204 No Content es estándar para eliminación exitosa
        } catch (ProgramNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (UnauthorizedException e) {
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inesperado al eliminar programa.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al eliminar programa.\"}").type(MediaType.APPLICATION_JSON).build();
        }
    }

    // Endpoint para cambiar el estado de compartir de un programa
    @PUT
    @Path("/programas/{programaId}/compartir")
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Cambia el estado de compartir de un programa", description = "Marca un programa como compartido o no compartido. Requiere autenticación y ser el dueño.")
    @APIResponse(responseCode = "200", description = "Estado de compartir actualizado exitosamente",
            content = @Content(schema = @Schema(implementation = ProgramaDTO.class)))
    @APIResponse(responseCode = "400", description = "Cuerpo de solicitud inválido (espera boolean)")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "403", description = "No autorizado (el usuario autenticado no es el dueño)")
    @APIResponse(responseCode = "404", description = "Programa no encontrado")
    @APIResponse(responseCode = "500", description = "Error en el servidor")
    // El cuerpo de la solicitud será un boolean simple: true para compartir, false para dejar de compartir
    @Consumes(MediaType.APPLICATION_JSON) // Consume JSON
    public Response updateProgramaSharingStatus(
            @PathParam("programaId") Long programaId,
            // Recibe un boolean simple en el cuerpo.
            boolean sharedStatus
    ) {
        // Obtener el ID del usuario autenticado
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            LOGGER.severe("-> updateProgramaSharingStatus: Endpoint protegido pero SecurityContext/Principal es null.");
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String authenticatedUserEmail = securityContext.getUserPrincipal().getName();
        Long authenticatedEstudianteId;
        try {
            authenticatedEstudianteId = estudianteService.getIdByEmail(authenticatedUserEmail);
            if (authenticatedEstudianteId == null) {
                LOGGER.severe("-> updateProgramaSharingStatus: Usuario autenticado con email '" + authenticatedUserEmail + "' no encontrado en DB.");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error interno: Usuario autenticado no encontrado.\"}").type(MediaType.APPLICATION_JSON).build();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "-> updateProgramaSharingStatus: ERROR inesperado al obtener ID del usuario autenticado.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al obtener información del usuario autenticado.\"}").type(MediaType.APPLICATION_JSON).build();
        }

        try {
            // Llamar al servicio para actualizar el estado de compartir
            ProgramaDTO updatedPrograma = programaService.updateSharingStatus(programaId, sharedStatus, authenticatedEstudianteId);
            return Response.ok(updatedPrograma).build();
        } catch (ProgramNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (UnauthorizedException e) {
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inesperado al actualizar estado de compartir de programa.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al actualizar estado de compartir.\"}").type(MediaType.APPLICATION_JSON).build();
        }
    }

    // ******************************************************
    // --- Lógica de Ejecución de Programas ---
    // ******************************************************

    // Endpoint para ejecutar un programa por su ID
    @POST
    @Path("/programas/{programaId}/ejecutar")
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Ejecuta un programa por ID", description = "Compila y ejecuta el código de un programa. Requiere autenticación y ser el dueño.")
    @APIResponse(responseCode = "200", description = "Ejecución completada",
            content = @Content(schema = @Schema(implementation = ProgramaExecutionResultDTO.class)))
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "403", description = "No autorizado (el usuario autenticado no es el dueño)")
    @APIResponse(responseCode = "404", description = "Programa no encontrado")
    @APIResponse(responseCode = "400", description = "Error de ejecución o compilación del código")
    @APIResponse(responseCode = "500", description = "Error interno del servidor al intentar ejecutar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response executePrograma(
            @PathParam("programaId") Long programaId
    ) {
        // Obtener el ID del usuario autenticado
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            LOGGER.severe("-> executePrograma: Endpoint protegido pero SecurityContext/Principal es null.");
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String authenticatedUserEmail = securityContext.getUserPrincipal().getName();
        Long authenticatedEstudianteId;
        try {
            authenticatedEstudianteId = estudianteService.getIdByEmail(authenticatedUserEmail);
            if (authenticatedEstudianteId == null) {
                LOGGER.severe("-> executePrograma: Usuario autenticado con email '" + authenticatedUserEmail + "' no encontrado en DB.");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error interno: Usuario autenticado no encontrado.\"}").type(MediaType.APPLICATION_JSON).build();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "-> executePrograma: ERROR inesperado al obtener ID del usuario autenticado.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al obtener información del usuario autenticado.\"}").type(MediaType.APPLICATION_JSON).build();
        }

        try {
            // Llama al servicio para ejecutar el programa
            ProgramaExecutionResultDTO result = programaService.executeProgram(programaId, authenticatedEstudianteId);
            return Response.ok(result).build();

        } catch (ProgramNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (UnauthorizedException e) {
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (ProgramExecutionException e) {
            if (e.getMessage().contains("Error de compilación") || e.getMessage().contains("código fuente vacío")) {
                // Para errores de compilación o código vacío
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"" + e.getMessage() + "\", \"details\": \"" + (e.getCause() != null ? e.getCause().getMessage() : "") + "\"}").type(MediaType.APPLICATION_JSON).build();
            } else if (e.getMessage().contains("Tiempo de ejecución excedido")) {
                // Para timeout de ejecución
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
            }
            else {
                LOGGER.log(Level.SEVERE, "Error inesperado durante el proceso de ejecución del programa.", e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error interno al ejecutar el programa: " + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
            }
        }
        catch (Exception e) {
            // Captura cualquier otra excepción inesperada
            LOGGER.log(Level.SEVERE, "Error inesperado en el endpoint de ejecución de programa.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al ejecutar programa.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }
}