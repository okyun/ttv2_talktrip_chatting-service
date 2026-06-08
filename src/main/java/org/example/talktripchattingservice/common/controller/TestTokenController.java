package org.example.talktripchattingservice.common.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@RestController
@RequestMapping("/api/member")
public class TestTokenController {

    private final SecretKey key;

    public TestTokenController(@Value("${jwt.secret-key}") String secretKey) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/test-token")
    public TestTokenResponse issueTestToken(
            @RequestParam(required = false, defaultValue = "k6-test@talktrip.local") String email,
            @RequestParam(required = false, defaultValue = "3600") long ttlSeconds
    ) {
        Instant now = Instant.now();
        Instant exp = now.plus(Math.max(60, ttlSeconds), ChronoUnit.SECONDS);

        String token = Jwts.builder()
                .subject(email)
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();

        return new TestTokenResponse(token);
    }

    public record TestTokenResponse(String accessToken) {}
}

