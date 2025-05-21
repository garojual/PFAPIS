package com.uq.security;

import io.jsonwebtoken.Claims;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.security.Principal;
import java.util.logging.Logger;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JWTAuthFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(JWTAuthFilter.class.getName());
    private static final String AUTHENTICATION_SCHEME = "Bearer";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Primero determinar si es un endpoint seguro
        boolean requiresAuth = isSecuredEndpoint(requestContext);

        // Si el endpoint no requiere autenticación, permitir el acceso sin verificar token
        if (!requiresAuth) {
            LOGGER.info("Acceso permitido a endpoint público sin autenticación");
            return;
        }

        // Si llegamos aquí, el endpoint requiere autenticación
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        LOGGER.info("Authorization header para endpoint seguro: " + (authorizationHeader == null ? "null" : authorizationHeader));

        // Verificar si hay header de autorización y es válido
        if (authorizationHeader == null || !authorizationHeader.startsWith(AUTHENTICATION_SCHEME)) {
            LOGGER.warning("Header de autorización ausente o sin prefijo Bearer para endpoint seguro");
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"No autorizado. Token ausente o formato incorrecto.\"}")
                    .build());
            return;
        }

        // Extraer y validar el token
        String token = authorizationHeader.substring(AUTHENTICATION_SCHEME.length()).trim();
        try {
            // Verificar y procesar el token
            final Claims claims = JWTUtil.validateToken(token);
            final String userEmail = claims.getSubject();

            LOGGER.info("Token JWT válido para usuario: " + userEmail);

            // Configurar el SecurityContext con la información del usuario autenticado
            final SecurityContext currentSecurityContext = requestContext.getSecurityContext();
            requestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return new Principal() {
                        @Override
                        public String getName() {
                            return userEmail;
                        }
                    };
                }

                @Override
                public boolean isUserInRole(String role) {
                    // Implementación de roles si es necesario
                    return true;
                }

                @Override
                public boolean isSecure() {
                    return currentSecurityContext.isSecure();
                }

                @Override
                public String getAuthenticationScheme() {
                    return AUTHENTICATION_SCHEME;
                }
            });

            LOGGER.info("SecurityContext actualizado con userEmail: " + userEmail);
        } catch (Exception e) {
            LOGGER.severe("Error al procesar token JWT: " + e.getMessage());
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Token inválido o expirado: " + e.getMessage() + "\"}")
                    .build());
        }
    }

    private boolean isSecuredEndpoint(ContainerRequestContext requestContext) {
        // Endpoints que no requieren autenticación
        String path = requestContext.getUriInfo().getPath();
        String method = requestContext.getMethod();

        LOGGER.info("Verificando seguridad para path: '" + path + "' con método: '" + method + "'");

        // Endpoints públicos (registro, login, verificación)
        // Comparar sin considerar barra inclinada al final
        if (path.equals("/estudiantes") && method.equals("POST")) {
            LOGGER.info("Endpoint de registro detectado - No requiere autenticación");
            return false; // Registro
        }
        if ((path.equals("/estudiantes/login") || path.equals("/estudiantes/login/")) && method.equals("POST")) {
            LOGGER.info("Endpoint de login detectado - No requiere autenticación");
            return false; // Login
        }
        if ((path.equals("/estudiantes/verificar") || path.equals("/estudiantes/verificar/")) && method.equals("POST")) {
            LOGGER.info("Endpoint de verificación detectado - No requiere autenticación");
            return false; // Verificación
        }

        if ((path.equals("/profesores/login") || path.equals("/profesores/login/")) && method.equals("POST")) {
            LOGGER.info("Endpoint de verificación detectado - No requiere autenticación");
            return false; // Login
        }

        // Swagger UI y OpenAPI
        if (path.startsWith("swagger") || path.startsWith("openapi") ||
                path.startsWith("q/") || path.contains("swagger") ||
                path.contains("openapi")) {
            LOGGER.info("Endpoint de documentación detectado - No requiere autenticación");
            return false;
        }

        // Por defecto, todos los demás endpoints requieren autenticación
        LOGGER.info("Endpoint seguro detectado - Requiere autenticación");
        return true;
    }
}