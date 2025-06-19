package com.rawello.maparserver.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Function;

@Component
public class JwtTokenProvider {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String ROLES_KEY = "roles";
    private static final String ROLE_USER = "ROLE_USER";
    private static final String ROLE_ANONYMOUS = "ROLE_ANONYMOUS";
    private static final String ROLE_OWNER = "ROLE_OWNER";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        Set<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        roles.add(ROLE_USER);
        roles.add(ROLE_OWNER);

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userDetails.getUsername());
        claims.put(ROLES_KEY, new ArrayList<>(roles));
        claims.put("created", now);

        logger.debug("Generating token for user: {}, roles: {}", userDetails.getUsername(), roles);

        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    public String generateAnonymousToken() {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        List<String> roles = Arrays.asList(ROLE_ANONYMOUS, ROLE_USER);
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "anonymous");
        claims.put(ROLES_KEY, roles);
        claims.put("created", now);

        logger.debug("Generating anonymous token with roles: {}", roles);

        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        String username = getClaimFromToken(token, Claims::getSubject);
        logger.debug("Extracted username from token: {}", username);
        return username;
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            List<String> roles = claims.get(ROLES_KEY, List.class);
            logger.debug("Extracted roles from token: {}", roles);
            return roles != null ? roles : Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error extracting roles from token", e);
            return Collections.emptyList();
        }
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
            logger.debug("Token claims: {}", claims);
            return claims;
        } catch (Exception e) {
            logger.error("Error parsing JWT claims", e);
            throw e;
        }
    }

    public Boolean isTokenExpired(String token) {
        try {
            final Date tokenExpirationDate = getExpirationDateFromToken(token);
            boolean isExpired = tokenExpirationDate.before(new Date());
            logger.debug("Token expiration check - expiration: {}, isExpired: {}", tokenExpirationDate, isExpired);
            return isExpired;
        } catch (Exception e) {
            logger.error("Error checking token expiration", e);
            return true;
        }
    }

    public boolean validateToken(String token) {
        try {
            if (token == null || token.isEmpty()) {
                logger.warn("Token is null or empty");
                return false;
            }

            Claims claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();

            boolean isValid = !isTokenExpired(token);
            boolean hasUsername = claims.getSubject() != null;
            boolean hasRoles = claims.get(ROLES_KEY) != null;

            logger.debug("Token validation - token: {}, isValid: {}, hasUsername: {}, hasRoles: {}, claims: {}",
                    token.substring(0, Math.min(10, token.length())),
                    isValid, hasUsername, hasRoles, claims);

            return isValid && hasUsername && hasRoles;
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            logger.warn("Expired JWT token: {}", ex.getMessage());
        } catch (io.jsonwebtoken.MalformedJwtException ex) {
            logger.warn("Invalid JWT token: {}", ex.getMessage());
        } catch (io.jsonwebtoken.UnsupportedJwtException ex) {
            logger.warn("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.warn("JWT claims string is empty: {}", ex.getMessage());
        } catch (Exception ex) {
            logger.error("Error validating token", ex);
        }
        return false;
    }
}