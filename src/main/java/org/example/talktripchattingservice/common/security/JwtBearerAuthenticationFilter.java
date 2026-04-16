package org.example.talktripchattingservice.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;

/**
 * REST(/api/chat/**)에서 {@code Authorization: Bearer} 로 전달된 JWT를 검증하고
 * {@link SecurityContextHolder} 에 인증을 넣어 {@link java.security.Principal} 주입이 동작하게 함.
 */
@Component
public class JwtBearerAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtBearerAuthenticationFilter.class);

    private final SecretKey key;

    public JwtBearerAuthenticationFilter(@Value("${jwt.secret-key}") String secretKey) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String auth = request.getHeader("Authorization");
            String token = extractBearer(auth);
            if (token != null && !token.isBlank()) {
                Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
                String email = resolveEmail(claims);
                if (email != null && !email.isBlank() && !"null".equalsIgnoreCase(email)) {
                    Principal principal = () -> email;
                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_U"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            log.debug("HTTP JWT 파싱 생략: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    private static String extractBearer(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        if (authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring("Bearer ".length()).trim();
        }
        return authorizationHeader.trim();
    }

    private static String resolveEmail(Claims claims) {
        Object email = claims.get("email");
        if (email != null && !String.valueOf(email).isBlank()) {
            return String.valueOf(email).trim();
        }
        Object sub = claims.get("sub");
        return sub != null ? String.valueOf(sub).trim() : null;
    }
}
