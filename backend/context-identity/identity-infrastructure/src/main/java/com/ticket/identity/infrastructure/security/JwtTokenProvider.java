package com.ticket.identity.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret:defaultSecretKeyWithAtLeast32BytesForHMACSHA256!}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, String role) {
        return generateToken(username, role, role, role, Collections.emptyList(), 0L);
    }

    public String generateToken(String username,
                                String role,
                                String customRole,
                                String roleName,
                                List<String> permissions) {
        return generateToken(username, role, customRole, roleName, permissions, 0L);
    }

    public String generateToken(String username,
                                String role,
                                String customRole,
                                String roleName,
                                List<String> permissions,
                                long tokenVersion) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        JwtBuilder builder = Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("customRole", customRole != null ? customRole : role)
                .claim("roleName", roleName != null ? roleName : (customRole != null ? customRole : role))
                .claim("tv", tokenVersion)
                .issuedAt(now)
                .expiration(expiryDate);

        if (permissions != null && !permissions.isEmpty()) {
            builder.claim("permissions", permissions);
        }

        return builder.signWith(getSigningKey()).compact();
    }

    public long getTokenVersionFromToken(String token) {
        try {
            Number tv = parseClaims(token).get("tv", Number.class);
            return tv != null ? tv.longValue() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public String getCustomRoleFromToken(String token) {
        try {
            String customRole = parseClaims(token).get("customRole", String.class);
            return (customRole != null && !customRole.isBlank()) ? customRole : getRoleFromToken(token);
        } catch (Exception e) {
            return getRoleFromToken(token);
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getPermissionsFromToken(String token) {
        try {
            Object raw = parseClaims(token).get("permissions");
            if (raw instanceof List<?> list) {
                return list.stream()
                        .filter(item -> item != null && !item.toString().isBlank())
                        .map(item -> item.toString().trim().toUpperCase())
                        .toList();
            }
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
