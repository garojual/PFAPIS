package com.uq.controller;

import com.uq.dto.*;
import com.uq.exception.*;
import com.uq.mapper.ProfesorMapper;
import com.uq.security.JWTUtil;
import com.uq.security.TokenResponse;
import com.uq.service.ComentarioService;
import com.uq.service.EjemploService;
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

    @Inject
    EjemploService ejemploService;


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

    // ******************************************************
    // --- Lógica de Compartir Ejemplos (Crear) ---
    // ******************************************************

    // Endpoint para que un profesor cree y comparta un nuevo ejemplo
    @POST
    @Path("/ejemplos")
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Crea un nuevo ejemplo de código", description = "Permite a un profesor crear y compartir un nuevo ejemplo de código Java. Requiere autenticación como profesor.")
    @APIResponse(responseCode = "201", description = "Ejemplo creado exitosamente",
            content = @Content(schema = @Schema(implementation = EjemploDTO.class)))
    @APIResponse(responseCode = "400", description = "Datos del ejemplo incompletos o formato inválido")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "403", description = "No autorizado (el usuario autenticado no es un profesor)")
    @APIResponse(responseCode = "500", description = "Error en el servidor")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createEjemplo(@Valid EjemploDTO request) {

        // Lógica de autorización: Verificar que el usuario autenticado es un profesor.
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            LOGGER.severe("-> createEjemplo: Endpoint protegido pero SecurityContext/Principal es null.");
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String authenticatedUserEmail = securityContext.getUserPrincipal().getName();
        Long authenticatedProfesorId;
        try {
            authenticatedProfesorId = profesorService.getIdByEmail(authenticatedUserEmail);
            LOGGER.info("-> createEjemplo: Acceso autorizado para profesor con ID: " + authenticatedProfesorId);

        } catch (UserNotFoundException e) {
            LOGGER.log(Level.WARNING, "-> createEjemplo: Intento de acceso por usuario autenticado pero no encontrado como Profesor: '" + authenticatedUserEmail + "'", e);
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"Acceso restringido a profesores.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "-> createEjemplo: ERROR inesperado al verificar la identidad del profesor.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error interno al verificar usuario.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        try {
            // Llamar al servicio para crear el ejemplo
            EjemploDTO createdEjemplo = ejemploService.createExampleByProfessor(request, authenticatedProfesorId);
            return Response.status(Response.Status.CREATED).entity(createdEjemplo).build();

        } catch (UserNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Profesor " + authenticatedProfesorId + " no encontrado en DB durante createExampleByProfessor (defensivo).", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error interno: Profesor creador no encontrado.\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inesperado al crear ejemplo.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al crear ejemplo.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // ******************************************************
    // --- Lógica de Gestión de Ejemplos (Editar/Eliminar) ---
    // ******************************************************

    // Endpoint para actualizar un ejemplo completo por su ID (PUT)
    @PUT
    @Path("/ejemplos/{ejemploId}")
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Actualiza un ejemplo de código (completo)", description = "Actualiza todos los campos de un ejemplo de código. Requiere autenticación como profesor y ser el dueño.")
    @APIResponse(responseCode = "200", description = "Ejemplo actualizado exitosamente",
            content = @Content(schema = @Schema(implementation = EjemploDTO.class)))
    @APIResponse(responseCode = "400", description = "Datos del ejemplo incompletos o formato inválido")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "403", description = "No autorizado (el usuario autenticado no es el dueño)")
    @APIResponse(responseCode = "404", description = "Ejemplo no encontrado")
    @APIResponse(responseCode = "500", description = "Error en el servidor")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateEjemplo(
            @PathParam("ejemploId") Long ejemploId,
            @Valid EjemploDTO request
    ) {
        // Lógica de autorización: Verificar que el usuario autenticado es un profesor Y el dueño del ejemplo
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            LOGGER.severe("-> updateEjemplo: Endpoint protegido pero SecurityContext/Principal es null.");
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String authenticatedUserEmail = securityContext.getUserPrincipal().getName();
        Long authenticatedProfesorId;
        try {
            authenticatedProfesorId = profesorService.getIdByEmail(authenticatedUserEmail);
            LOGGER.info("-> updateEjemplo: Acceso autorizado para profesor con ID: " + authenticatedProfesorId);
        } catch (UserNotFoundException e) {
            LOGGER.log(Level.WARNING, "-> updateEjemplo: Intento de acceso por usuario autenticado pero no encontrado como Profesor: '" + authenticatedUserEmail + "'", e);
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"Acceso restringido a profesores.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "-> updateEjemplo: ERROR inesperado al verificar la identidad del profesor.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error interno al verificar usuario.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        // Si la autorización como profesor pasa, procedemos a actualizar el ejemplo (el servicio verificará la propiedad)
        try {
            EjemploDTO updatedEjemplo = ejemploService.updateExample(ejemploId, request, authenticatedProfesorId);
            return Response.ok(updatedEjemplo).build(); // 200 OK con el ejemplo actualizado

        } catch (ExampleNotFoundException e) { // El ejemplo no existe
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (UnauthorizedException e) { // El profesor no es el dueño (lanzado por el servicio)
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (IllegalArgumentException e) { // Validaciones del DTO o del service
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inesperado al actualizar ejemplo completo con ID " + ejemploId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al actualizar ejemplo.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // Endpoint para actualizar un ejemplo parcialmente por su ID (PATCH)
    @PATCH
    @Path("/ejemplos/{ejemploId}")
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Actualiza un ejemplo de código (parcial)", description = "Actualiza campos específicos de un ejemplo de código. Requiere autenticación como profesor y ser el dueño.")
    @APIResponse(responseCode = "200", description = "Ejemplo actualizado exitosamente",
            content = @Content(schema = @Schema(implementation = EjemploDTO.class)))
    @APIResponse(responseCode = "400", description = "Datos del ejemplo incompletos o formato inválido")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "403", description = "No autorizado (el usuario autenticado no es el dueño)")
    @APIResponse(responseCode = "404", description = "Ejemplo no encontrado")
    @APIResponse(responseCode = "500", description = "Error en el servidor")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response partialUpdateEjemplo(
            @PathParam("ejemploId") Long ejemploId,
            EjemploDTO request
    ) {
        // Lógica de autorización: Verificar que el usuario autenticado es un profesor Y el dueño del ejemplo
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            LOGGER.severe("-> partialUpdateEjemplo: Endpoint protegido pero SecurityContext/Principal es null.");
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String authenticatedUserEmail = securityContext.getUserPrincipal().getName();
        Long authenticatedProfesorId;
        try {
            authenticatedProfesorId = profesorService.getIdByEmail(authenticatedUserEmail);
            LOGGER.info("-> partialUpdateEjemplo: Acceso autorizado para profesor con ID: " + authenticatedProfesorId);
        } catch (UserNotFoundException e) {
            LOGGER.log(Level.WARNING, "-> partialUpdateEjemplo: Intento de acceso por usuario autenticado pero no encontrado como Profesor: '" + authenticatedUserEmail + "'", e);
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"Acceso restringido a profesores.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "-> partialUpdateEjemplo: ERROR inesperado al verificar la identidad del profesor.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error interno al verificar usuario.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        try {
            EjemploDTO updatedEjemplo = ejemploService.partialUpdateExample(ejemploId, request, authenticatedProfesorId);
            return Response.ok(updatedEjemplo).build(); // 200 OK con el ejemplo actualizado

        } catch (ExampleNotFoundException e) { // El ejemplo no existe
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (UnauthorizedException e) { // El profesor no es el dueño (lanzado por el servicio)
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (IllegalArgumentException e) { // Validaciones del DTO o del service
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inesperado al actualizar ejemplo parcialmente con ID " + ejemploId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al actualizar ejemplo parcialmente.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }


    // Endpoint para eliminar un ejemplo por su ID (DELETE)
    @DELETE
    @Path("/ejemplos/{ejemploId}")
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Elimina un ejemplo de código", description = "Elimina un ejemplo de código del sistema. Requiere autenticación como profesor y ser el dueño.")
    @APIResponse(responseCode = "204", description = "Ejemplo eliminado exitosamente")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "403", description = "No autorizado (el usuario autenticado no es el dueño)")
    @APIResponse(responseCode = "404", description = "Ejemplo no encontrado")
    @APIResponse(responseCode = "500", description = "Error en el servidor")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteEjemplo(
            @PathParam("ejemploId") Long ejemploId
    ) {
        // Lógica de autorización: Verificar que el usuario autenticado es un profesor Y el dueño del ejemplo
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            LOGGER.severe("-> deleteEjemplo: Endpoint protegido pero SecurityContext/Principal es null.");
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String authenticatedUserEmail = securityContext.getUserPrincipal().getName();
        Long authenticatedProfesorId;
        try {
            authenticatedProfesorId = profesorService.getIdByEmail(authenticatedUserEmail);
            LOGGER.info("-> deleteEjemplo: Acceso autorizado para profesor con ID: " + authenticatedProfesorId);
        } catch (UserNotFoundException e) {
            LOGGER.log(Level.WARNING, "-> deleteEjemplo: Intento de acceso por usuario autenticado pero no encontrado como Profesor: '" + authenticatedUserEmail + "'", e);
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"Acceso restringido a profesores.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "-> deleteEjemplo: ERROR inesperado al verificar la identidad del profesor.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error interno al verificar usuario.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        try {
            ejemploService.deleteExample(ejemploId, authenticatedProfesorId);
            return Response.noContent().build(); // 204 No Content

        } catch (ExampleNotFoundException e) { // El ejemplo no existe
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        } catch (UnauthorizedException e) { // El profesor no es el dueño (lanzado por el servicio)
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\": \"" + e.getMessage() + "\"}").type(MediaType.APPLICATION_JSON).build();
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inesperado al eliminar ejemplo con ID " + ejemploId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Error en el servidor al eliminar ejemplo.\"}")
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

}
