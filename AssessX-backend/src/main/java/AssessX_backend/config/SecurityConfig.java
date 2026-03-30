package AssessX_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    public SecurityConfig(OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    /**
     * Chain 1 (вищий пріоритет): OAuth2 login flow.
     * Сесія потрібна для CSRF-state параметру GitHub OAuth.
     * Після успішного логіну — повертає JWT у JSON.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain oauth2FilterChain(HttpSecurity http) throws Exception {
        http
            // /auth/github    → Spring Security OAuth2AuthorizationRequestRedirectFilter
            //                   (baseUri="/auth" + registrationId="github" = /auth/github)
            // /auth/github/callback → OAuth2LoginAuthenticationFilter (callback від GitHub)
            .securityMatcher("/auth/github", "/auth/github/callback",
                    "/login/oauth2/**", "/oauth2/**")
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(endpoint ->
                    endpoint.baseUri("/auth"))
                .redirectionEndpoint(endpoint ->
                    endpoint.baseUri("/auth/github/callback"))
                .successHandler(oAuth2LoginSuccessHandler)
            );
        return http.build();
    }

    /**
     * Chain 2: всі решта ендпоінти — stateless JWT Bearer.
     * /api/**, /auth/me, /auth/complete-registration — потребують валідного токену.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
            );
        return http.build();
    }
}
