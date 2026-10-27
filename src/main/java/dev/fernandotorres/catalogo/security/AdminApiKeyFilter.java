package dev.fernandotorres.catalogo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class AdminApiKeyFilter extends OncePerRequestFilter {

    private static final String ADMIN_PATH = "/api/v1/admin/";
    private static final String API_KEY_HEADER = "X-Admin-Key";

    private final String configuredKey;

    public AdminApiKeyFilter(@Value("${catalog.admin.api-key:}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith(ADMIN_PATH);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (configuredKey.isBlank()) {
            response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "Admin API desativada: configure ADMIN_API_KEY");
            return;
        }

        String suppliedKey = request.getHeader(API_KEY_HEADER);
        if (suppliedKey == null || !MessageDigest.isEqual(
                configuredKey.getBytes(StandardCharsets.UTF_8),
                suppliedKey.getBytes(StandardCharsets.UTF_8)
        )) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Chave administrativa ausente ou inválida");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
