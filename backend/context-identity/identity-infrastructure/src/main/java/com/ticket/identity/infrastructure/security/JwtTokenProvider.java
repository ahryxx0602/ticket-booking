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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret:defaultSecretKeyWithAtLeast32BytesForHMACSHA256!}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    /**
     * BE-SEC-001: Lưu mốc thời gian thu hồi token (epoch millis) cho từng username khi Admin thực hiện
     * resetPassword, lock tài khoản hoặc thay đổi vai trò, đảm bảo các JWT cấp trước thời điểm này bị thu hồi.
     */
    private static final Map<String, Long> USER_TOKEN_REVOCATION_TIMESTAMPS = new ConcurrentHashMap<>();

    public static void revokeTokensIssuedBeforeNow(String username) {
        if (username != null && !username.isBlank()) {
            USER_TOKEN_REVOCATION_TIMESTAMPS.put(username.trim().toLowerCase(), System.currentTimeMillis());
        }
    }

    public static void clearRevocationForUser(String username) {
        if (username != null && !username.isBlank()) {
            USER_TOKEN_REVOCATION_TIMESTAMPS.remove(username.trim().toLowerCase());
        }
    }

    public boolean isTokenRevokedForUser(String username, String token) {
        if (username == null || username.isBlank()) {
            return true;
        }
        Long revokedAtMs = USER_TOKEN_REVOCATION_TIMESTAMPS.get(username.trim().toLowerCase());
        if (revokedAtMs == null) {
            return false;
        }
        try {
            Date issuedAt = parseClaims(token).getIssuedAt();
            // JJWT truncates iat to seconds precision; if issuedAt is null or before/at revocation timestamp, token is revoked
            return issuedAt == null || issuedAt.getTime() <= revokedAtMs;
        } catch (Exception e) {
            return true;
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, String role) {
        return generateToken(username, role, role, role, Collections.emptyList());
    }

    public String generateToken(String username,
                                String role,
                                String customRole,
                                String roleName,
                                List<String> permissions) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        JwtBuilder builder = Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("customRole", customRole != null ? customRole : role)
                .claim("roleName", roleName != null ? roleName : (customRole != null ? customRole : role))
                .issuedAt(now)
                .expiration(expiryDate);

        if (permissions != null && !permissions.isEmpty()) {
            builder.claim("permissions", permissions);
        }

        return builder.signWith(getSigningKey()).compact();
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
