package AssessX_backend.config;

import AssessX_backend.model.User;
import AssessX_backend.repository.UserRepository;
import AssessX_backend.service.JwtTokenProvider;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OAuth2LoginSuccessHandler(UserRepository userRepository,
                                     JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Long githubId = ((Number) oAuth2User.getAttribute("id")).longValue();
        String githubLogin = oAuth2User.getAttribute("login");
        String name = oAuth2User.getAttribute("name");

        if (name == null || name.isBlank()) {
            name = githubLogin;
        }
        final String finalName = name;

        User user = userRepository.findByGithubId(githubId).orElseGet(() -> {
            User newUser = new User();
            newUser.setGithubId(githubId);
            newUser.setGithubLogin(githubLogin);
            newUser.setName(finalName);
            newUser.setRole(User.Role.STUDENT);
            return userRepository.save(newUser);
        });

        String token = jwtTokenProvider.generateToken(
                user.getId(), user.getGithubLogin(), user.getRole().name());

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Map.of("token", token)));
    }
}