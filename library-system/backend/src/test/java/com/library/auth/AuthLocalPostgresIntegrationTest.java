package com.library.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.enums.UserRole;
import com.library.common.enums.UserStatus;
import com.library.reader.entity.ReaderType;
import com.library.reader.repository.ReaderTypeRepository;
import com.library.user.entity.User;
import com.library.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.net.Socket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fallback khi Testcontainers/Docker API không dùng được trên Windows.
 * Yêu cầu Postgres localhost:5432 (docker-compose) user/pass/db = library.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIf("com.library.auth.AuthLocalPostgresIntegrationTest#postgresAvailable")
class AuthLocalPostgresIntegrationTest {

    public static boolean postgresAvailable() {
        try (Socket socket = new Socket("localhost", 5432)) {
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5432/library");
        registry.add("spring.datasource.username", () -> "library");
        registry.add("spring.datasource.password", () -> "library");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("library.jwt.secret", () -> "test-library-jwt-secret-key-min-32-chars!!");
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> "1025");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ReaderTypeRepository readerTypeRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (readerTypeRepository.findByCode("STUDENT").isEmpty()) {
            ReaderType type = new ReaderType();
            type.setCode("STUDENT");
            type.setName("Sinh viên");
            type.setMaxBooks(5);
            type.setLoanDays(14);
            type.setMaxRenewals(2);
            type.setRenewDays(7);
            type.setMaxReservations(3);
            type.setCardFee(BigDecimal.valueOf(20000));
            type.setCardValidityMonths(12);
            type.setActive(true);
            readerTypeRepository.save(type);
        }
        upsertUser("admin@test.vn", "0911000001", "Admin Test", UserRole.ADMIN, "Admin@123");
        upsertUser("user@test.vn", "0911000002", "User Test", UserRole.USER, "User@123");
    }

    private void upsertUser(String email, String phone, String fullName, UserRole role, String password) {
        User user = userRepository.findByEmailIgnoreCase(email).orElseGet(User::new);
        user.setEmail(email);
        user.setPhone(phone);
        user.setFullName(fullName);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    @Test
    void registerLoginAndMe() throws Exception {
        String email = "newuser" + System.currentTimeMillis() + "@test.vn";
        String phone = "0987" + String.format("%06d", (int) (System.currentTimeMillis() % 1_000_000));
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Van A",
                                  "email": "%s",
                                  "phone": "%s",
                                  "password": "User@123",
                                  "readerTypeCode": "STUDENT"
                                }
                                """.formatted(email, phone)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        User registered = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        registered.setStatus(UserStatus.ACTIVE);
        userRepository.save(registered);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"User@123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        JsonNode data = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("data");
        String accessToken = data.get("accessToken").asText();
        assertThat(accessToken).isNotBlank();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));
    }

    @Test
    void userForbiddenOnAuditLogs_adminAllowed() throws Exception {
        String userToken = login("user@test.vn", "User@123");
        mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        String adminToken = login("admin@test.vn", "Admin@123");
        mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }
}
