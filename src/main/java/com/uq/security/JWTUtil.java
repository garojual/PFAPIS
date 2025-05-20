package com.uq.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JWTUtil {
    // Clave secreta para firmar el token (debe ser >= 256 bits)
    private static final String SECRET_KEY = "secreto_super_seguro_secreto_super_seguro_1234567890";
    private static final long EXPIRATION_TIME = 86400000; // 1 día en milisegundos

    // Convertir la clave secreta en un SecretKey
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    /**
     * Genera un token JWT con el email y el rol del usuario.
     *
     * @param email El email del usuario.
     * @return El token JWT generado.
     */
    public static String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email) // Sujeto del token (email del usuario)
                .setIssuedAt(new Date()) // Fecha de emisión del token
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // Fecha de expiración
                .signWith(KEY, SignatureAlgorithm.HS256) // Firmar el token con la clave secreta
                .compact(); // Generar el token como una cadena compacta
    }

    /**
     * Valida un token JWT y devuelve las reclamaciones (claims).
     *
     * @param token El token JWT a validar.
     * @return Las reclamaciones (claims) del token.
     */
    public static Claims validateToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(KEY) // Clave secreta para validar la firma
                .build()
                .parseClaimsJws(token) // Validar y parsear el token
                .getBody(); // Obtener las reclamaciones (claims)
    }
}
