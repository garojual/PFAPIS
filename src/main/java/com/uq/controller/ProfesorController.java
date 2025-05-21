package com.uq.controller;

import com.uq.dto.*;
import com.uq.exception.InactiveAccountException;
import com.uq.exception.InvalidCredentialsException;
import com.uq.exception.ProgramNotFoundException;
import com.uq.exception.UserNotFoundException;
import com.uq.mapper.ProfesorMapper;
import com.uq.security.JWTUtil;
import com.uq.security.TokenResponse;
import com.uq.service.ComentarioService;
import com.uq.service.ProfesorService;
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
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@SecurityScheme(
        securitySchemeName = "jwtAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
@Path("/profesores")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Gestión de Profesores", description = "API para gestionar profesores.")
public class ProfesorController {

    private static final Logger LOGGER = Logger.getLogger(ProfesorController.class.getName());

    @Inject
    ProfesorService profesorService;

    @Inject
    ProfesorMapper profesorMapper;

    @Context
    SecurityContext securityContext;

    @Inject
    ComentarioService comentarioService;


    @PUT
    @Path("/{id}")
    @SecurityRequirement(name = "jwtAuth") // Requiere autenticación con JWT
    @Operation(summary = "Actualiza un profesor existente (completo)", description = "Actualiza todos los campos de un usuario.")
    @APIResponse(responseCode = "200", description = "Usuario actualizado exitosamente")
    @APIResponse(responseCode = "400", description = "Datos incompletos o formato inválido")
    @APIResponse(responseCode = "401", description = "No autenticado (Falta JWT o es inválido)")
    @APIResponse(responseCode = "404", description = "Usuario no encontrado")
    @APIResponse(responseCode = "500", description = "Error en el servidor")
    public Response updateUser(@PathParam("id") Long id, @Valid ProfesorDTO request) {

        if (!isAuthorizedProfesor(id)) {
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"No autorizado para modificar este usuario.\"}").type(MediaType.APPLICATION_JSON).build();
        }
        try {
            UserResponse updatedUser = profesorService.updateProfesor(id, profesorMapper.toEntity(request));
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
    @SecurityRequirement(name = "jwtAuth") // Requiere autenticación con JWT
    @Operation(summary = "Actualiza un profesor existente (parcial)", description = "Actualiza campos específicos de un usuario.")
    @APIResponse(responseCode = "200", description = "Usuario actualizado exitosamente")
    @APIResponse(responseCode = "400", description = "Datos incompletos o formato inválido")
    @APIResponse(responseCode = "401", description = "No autenticado (Falta JWT o es inválido)")
    @APIResponse(responseCode = "403", description = "No autorizado (el usuario autenticado no coincide con el ID)")
    @APIResponse(responseCode = "404", description = "Usuario no encontrado")
    @APIResponse(responseCode = "500", description = "Error en el servidor")
    public Response partialUpdateUser(@PathParam("id") Long id, ProfesorDTO request) {
        if (!isAuthorizedProfesor(id)) {
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"No autorizado para modificar este usuario.\"}").type(MediaType.APPLICATION_JSON).build();
        }
        try {
            UserResponse updatedUser = profesorService.partialUpdateProfesor(id, profesorMapper.toEntity(request));
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
    @SecurityRequirement(name = "jwtAuth") // Requiere autenticación con JWT
    @Operation(summary = "Elimina un profesor por ID", description = "Elimina un usuario del sistema.")
    @APIResponse(responseCode = "204", description = "Usuario eliminado exitosamente")
    @APIResponse(responseCode = "401", description = "No autenticado (Falta JWT o es inválido)")
    @APIResponse(responseCode = "403", description = "No autorizado (el usuario autenticado no coincide con el ID)")
    @APIResponse(responseCode = "404", description = "Usuario no encontrado")
    @APIResponse(responseCode = "500", description = "Error en el servidor")
    public Response deleteUser(@PathParam("id") Long id, ProfesorDTO request) {
        if (!isAuthorizedProfesor(id)) {
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"No autorizado para modificar este usuario.\"}").type(MediaType.APPLICATION_JSON).build();
        }
        try {
            UserResponse updatedUser = profesorService.partialUpdateProfesor(id, profesorMapper.toEntity(request));
            return Response.ok(updatedUser).build();
        } catch (UserNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor durante la actualización parcial.\"}").type(MediaType.APPLICATION_JSON).build();
        }
    }


    // Metodo auxiliar para verificar autorización de Profesor (para endpoints /profesores/{id})
    private boolean isAuthorizedProfesor(Long requestedProfesorId) {
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            LOGGER.warning("-> isAuthorizedProfesor: SecurityContext o Principal es null. Retornando false.");
            return false;
        }
        String authenticatedUserEmail = securityContext.getUserPrincipal().getName();
        try {
            Long authenticatedProfesorId = profesorService.getIdByEmail(authenticatedUserEmail);
            boolean isMatch = requestedProfesorId != null && authenticatedProfesorId != null && authenticatedProfesorId.equals(requestedProfesorId);
            return isMatch;
        } catch (UserNotFoundException e) {
            LOGGER.log(Level.SEVERE, "-> isAuthorizedProfesor: ERROR: Usuario autenticado con email '" + authenticatedUserEmail + "' no encontrado en DB.", e);
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "-> isAuthorizedProfesor: ERROR inesperado al obtener ID del usuario autenticado por email.", e);
            return false;
        }
    }

    @POST
    @Path("/login")
    @Tag(name = "Login")
    @Operation(summary = "Iniciar sesión", description = "Inicia sesión y genera el token para acceder a los demás endpoints.")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "200", description = "Inicio de sesión exitoso. Token generado.")
    @APIResponse(responseCode = "401", description = "Credenciales inválidas.")
    @APIResponse(responseCode = "404", description = "Usuario no encontrado.")
    @APIResponse(responseCode = "500", description = "Error en el servidor.")
    public Response login(@Valid ProfesorLoginDTO request) {
        try {
            String userEmail = profesorService.login(request.getEmail(), request.getContrasena());
            String jwtToken = JWTUtil.generateToken(userEmail);
            return Response.ok().entity(new TokenResponse(jwtToken)).build();

        } catch (UserNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (InvalidCredentialsException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        }
        catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Error en el servidor durante el inicio de sesión.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }


    // ******************************************************
    // --- Lógica de Revisión de Programas (Ver Todos) ---
    // ******************************************************

    // Endpoint para que un profesor vea todos los programas de estudiantes
    @GET
    @Path("/programas")
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Obtiene todos los programas de los estudiantes para revisión", description = "Lista todos los programas creados por todos los estudiantes. Requiere autenticación como profesor.")
    @APIResponse(responseCode = "200", description = "Lista de programas obtenida exitosamente",
            content = @Content(schema = @Schema(implementation = ProgramaDTO.class))) // Lista de ProgramaDTO
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "403", description = "No autorizado (el usuario autenticado no es un profesor)")
    @APIResponse(responseCode = "500", description = "Error en el servidor")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getAllStudentProgramsForReview() {

        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            LOGGER.severe("-> getAllStudentProgramsForReview: Endpoint protegido pero SecurityContext/Principal es null.");
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String authenticatedUserEmail = securityContext.getUserPrincipal().getName();
        try {
            Long authenticatedProfesorId = profesorService.getIdByEmail(authenticatedUserEmail);
            LOGGER.info("-> getAllStudentProgramsForReview: Acceso autorizado para profesor con ID: " + authenticatedProfesorId);

        } catch (UserNotFoundException e) {
            // Si el email del token no corresponde a un profesor en DB, es un acceso no autorizado (probablemente un estudiante con token de profesor)
            LOGGER.log(Level.WARNING, "-> getAllStudentProgramsForReview: Intento de acceso por usuario autenticado pero no encontrado como Profesor: '" + authenticatedUserEmail + "'", e);
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"Acceso restringido a profesores.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "-> getAllStudentProgramsForReview: ERROR inesperado al verificar la identidad del profesor.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error interno al verificar usuario.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }


        // Si la autorización pasa, procedemos a obtener todos los programas
        try {
            // Llamar al servicio (ProgramaService) para obtener todos los programas
            List<ProgramaDTO> programas = profesorService.listAllPrograms();
            return Response.ok(programas).build(); // Retorna la lista de programas

        } catch (Exception e) {
            // Captura cualquier error durante la obtención de programas
            LOGGER.log(Level.SEVERE, "Error inesperado al obtener todos los programas de estudiantes.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al obtener los programas.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // ******************************************************
    // --- Lógica de Revisión de Programas (Añadir Comentarios) ---
    // ******************************************************

    // Endpoint para que un profesor añada un comentario a un programa específico
    @POST
    @Path("/programas/{programaId}/comentarios")
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Añade un comentario a un programa de estudiante", description = "Permite a un profesor dejar un comentario en el código de un programa de estudiante. Requiere autenticación como profesor.")
    @APIResponse(responseCode = "201", description = "Comentario creado exitosamente",
            content = @Content(schema = @Schema(implementation = ComentarioDTO.class)))
    @APIResponse(responseCode = "400", description = "Datos del comentario incompletos o formato inválido")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "403", description = "No autorizado (el usuario autenticado no es un profesor)")
    @APIResponse(responseCode = "404", description = "Programa no encontrado")
    @APIResponse(responseCode = "500", description = "Error en el servidor")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addComentarioToPrograma(
            @PathParam("programaId") Long programaId,
            @Valid ComentarioRequestDTO request
    ) {
        // Lógica de autorización: Verificar que el usuario autenticado es un profesor.
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            LOGGER.severe("-> addComentarioToPrograma: Endpoint protegido pero SecurityContext/Principal es null.");
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String authenticatedUserEmail = securityContext.getUserPrincipal().getName();
        Long authenticatedProfesorId;
        try {
            authenticatedProfesorId = profesorService.getIdByEmail(authenticatedUserEmail);
            LOGGER.info("-> addComentarioToPrograma: Acceso autorizado para profesor con ID: " + authenticatedProfesorId);

        } catch (UserNotFoundException e) {
            LOGGER.log(Level.WARNING, "-> addComentarioToPrograma: Intento de acceso por usuario autenticado pero no encontrado como Profesor: '" + authenticatedUserEmail + "'", e);
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"Acceso restringido a profesores.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "-> addComentarioToPrograma: ERROR inesperado al verificar la identidad del profesor.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error interno al verificar usuario.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        // Si la autorización como profesor pasa, procedemos a añadir el comentario
        try {
            // Llamar al servicio para añadir el comentario
            ComentarioDTO createdComentario = comentarioService.addCommentByProfessor(
                    programaId,
                    authenticatedProfesorId,
                    request.getTexto()
            );
            return Response.status(Response.Status.CREATED).entity(createdComentario).build();

        } catch (ProgramNotFoundException e) {
            // El programa no existe
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (UserNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Profesor " + authenticatedProfesorId + " no encontrado en DB durante addCommentByProfessor.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error interno: Profesor comentando no encontrado.\"}").type(MediaType.APPLICATION_JSON).build();
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inesperado al añadir comentario al programa " + programaId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al añadir comentario.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

}
