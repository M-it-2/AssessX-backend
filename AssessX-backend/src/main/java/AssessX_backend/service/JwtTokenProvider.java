package AssessX_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Централізований провайдер JWT: генерація, валідація, витяг клеймів.
 *
 * <p>Структура токена:
 * <ul>
 *   <li>{@code sub}   — userId (рядок)</li>
 *   <li>{@code login} — GitHub login</li>
 *   <li>{@code role}  — "STUDENT" або "TEACHER"</li>
 *   <li>{@code iat}   — час видачі</li>
 *   <li>{@code exp}   — час закінчення дії</li>
 * </ul>
 *
 * <p>Для перевірки токенів у вхідних API-запитах Spring Security
 * викликає {@link JwtDecoder} автоматично через фільтр-ланцюг.
 * {@link #parseToken(String)} призначений для явної валідації в бізнес-логіці
 * та тестах.
 */
@Service
public class JwtTokenProvider {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public JwtTokenProvider(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
    }

    // -------------------------------------------------------------------------
    // Генерація
    // -------------------------------------------------------------------------

    /**
     * Генерує підписаний JWT для вказаного користувача.
     *
     * @param userId      первинний ключ з таблиці {@code users}
     * @param githubLogin логін на GitHub
     * @param role        "STUDENT" або "TEACHER"
     * @return компактний рядок токена (header.payload.signature)
     */
    public String generateToken(Long userId, String githubLogin, String role) {
        Instant now = Instant.now();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiresAt(now.plusMillis(expirationMs))
                .claim("login", githubLogin)
                .claim("role", role)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    // -------------------------------------------------------------------------
    // Валідація / парсинг
    // -------------------------------------------------------------------------

    /**
     * Декодує та валідує токен: перевіряє підпис і термін дії.
     *
     * @param token компактний рядок JWT
     * @return розпарсений {@link Jwt} зі всіма клеймами
     * @throws JwtException якщо підпис недійсний або токен прострочений
     */
    public Jwt parseToken(String token) {
        return jwtDecoder.decode(token);
    }

    /**
     * Повертає {@code true}, якщо токен має дійсний підпис і не прострочений.
     */
    public boolean isValid(String token) {
        try {
            jwtDecoder.decode(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Витяг клеймів
    // -------------------------------------------------------------------------

    /** Повертає userId з клейму {@code sub}. */
    public Long getUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    /** Повертає GitHub login з клейму {@code login}. */
    public String getLogin(Jwt jwt) {
        return jwt.getClaimAsString("login");
    }

    /** Повертає роль з клейму {@code role} ("STUDENT" або "TEACHER"). */
    public String getRole(Jwt jwt) {
        return jwt.getClaimAsString("role");
    }
}
