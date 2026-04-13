package com.nc.sinpase.poc.modulith.covoit.auth.adapters.out.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.io.IOException;
import java.util.Base64;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (jwt != null && !jwt.isEmpty()) {
                // Décoder le JWT pour extraire le sub (userId)
                String userId = extractUserIdFromJwt(jwt);

                if (userId != null && !userId.isEmpty()) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, null);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String extractUserIdFromJwt(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length >= 2) {
                String payload = parts[1];
                // Ajouter padding si nécessaire
                while (payload.length() % 4 != 0) {
                    payload += "=";
                }
                byte[] decodedBytes = Base64.getDecoder().decode(payload);
                String decodedPayload = new String(decodedBytes);

                // Extraire "sub" du JSON (simple regex)
                if (decodedPayload.contains("\"sub\"")) {
                    int start = decodedPayload.indexOf("\"sub\":\"") + 7;
                    int end = decodedPayload.indexOf("\"", start);
                    return decodedPayload.substring(start, end);
                }
            }
        } catch (Exception e) {
            logger.error("Error extracting userId from JWT", e);
        }
        return null;
    }
}