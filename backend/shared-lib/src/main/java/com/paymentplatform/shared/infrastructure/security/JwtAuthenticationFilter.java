package com.paymentplatform.shared.infrastructure.security;

import com.paymentplatform.shared.domain.security.PermissionCatalog;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtre d'authentification JWT : valide le token, puis expose les permissions
 * (RBAC) comme authorities. Un token invalide ⇒ aucun contexte (401 géré en aval).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                AuthenticatedUser user = jwtService.parse(header.substring(7));
                List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>(user.roles().stream()
                        .map(r -> new SimpleGrantedAuthority(r))
                        .toList());
                authorities.addAll(PermissionCatalog.permissionsFor(user.roles()).stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList());
                var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}