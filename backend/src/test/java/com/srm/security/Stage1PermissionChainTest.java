package com.srm.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srm.security.auth.UserAccountRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import com.srm.system.infrastructure.persistence.entity.SysMessageEntity;
import com.srm.system.infrastructure.persistence.mapper.SysMessageMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Stage1PermissionChainTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private SysMessageMapper messageMapper;

    private static final String AUDITOR_USER = "audit_chain";
    private static final String AUDITOR_PASSWORD = "AuditorChainTest!2026";
    private static final String INTERNAL_USER = "internal_message_chain";
    private static final String INTERNAL_PASSWORD = "InternalMessageTest!2026";
    private long internalMessageId;
    private long anotherUsersMessageId;

    @BeforeAll
    void seedAuditor() {
        long id = users.createUserIfMissing(AUDITOR_USER, passwordEncoder.encode(AUDITOR_PASSWORD), "Chain Auditor", "test");
        users.assignRoleIfMissing(id, "INTERNAL_AUDITOR", "test");
        long adminId = users.createUserIfMissing("chain_admin", passwordEncoder.encode("ChainAdminTest!2026"), "Chain Admin", "test");
        users.assignRoleIfMissing(adminId, "SUPER_ADMIN", "test");
        long internalId = users.createUserIfMissing(INTERNAL_USER,
                passwordEncoder.encode(INTERNAL_PASSWORD), "Message User", "test");
        users.assignRoleIfMissing(internalId, "INTERNAL_USER", "test");
        SysMessageEntity message = new SysMessageEntity();
        message.setRecipientId(internalId);
        message.setTitle("Permission chain message");
        message.setContent("owner only");
        message.setMessageType("INFO");
        message.setStatus("UNREAD");
        message.setSourceType("TEST");
        message.setSourceId("message-permission-chain");
        message.setIdempotencyKey("message-permission-chain");
        message.setCreatedAt(java.time.LocalDateTime.now());
        messageMapper.insert(message);
        internalMessageId = message.getId();
        SysMessageEntity another = new SysMessageEntity();
        another.setRecipientId(adminId);
        another.setTitle("Another user's message");
        another.setContent("must remain private");
        another.setMessageType("INFO");
        another.setStatus("UNREAD");
        another.setSourceType("TEST");
        another.setSourceId("another-message-permission-chain");
        another.setIdempotencyKey("another-message-permission-chain");
        another.setCreatedAt(java.time.LocalDateTime.now());
        messageMapper.insert(another);
        anotherUsersMessageId = another.getId();
    }

    @Test @Order(1)
    void readOnlyUserCanQueryButAllWritesReturn403() throws Exception {
        String token = accessToken(login(AUDITOR_USER, AUDITOR_PASSWORD));

        mockMvc.perform(get("/api/v1/system/users").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/system/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"hacker\",\"password\":\"HackerPassword!2026\",\"displayName\":\"H\",\"mainOrganizationId\":1,\"mainDepartmentId\":1,\"mainPositionId\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PLATFORM_ACCESS_DENIED"));

        mockMvc.perform(put("/api/v1/system/users/1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Hacked\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/system/users/1/enable")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/system/users/1/disable")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/system/inbox-events/1/retry")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/system/roles/1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userIds\":[1]}"))
                .andExpect(status().isForbidden());
    }

    @Test @Order(2)
    void noPermissionUserGets403OnProtectedEndpoint() throws Exception {
        String token = accessToken(login("stage0_viewer", "Stage0ViewerTestOnly!2026"));

        mockMvc.perform(get("/api/v1/system/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/master-data/materials")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test @Order(3)
    void adminHasFullAccessAsControl() throws Exception {
        String token = accessToken(login("stage0_admin", "Stage0AdminTestOnly!2026"));

        mockMvc.perform(get("/api/v1/system/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/master-data/materials")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    @Test @Order(4)
    void attachmentDownloadAndReplaceRequirePermission() throws Exception {
        String adminToken = accessToken(login("stage0_admin", "Stage0AdminTestOnly!2026"));
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf",
                "%PDF-1.4\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        MvcResult up = mockMvc.perform(multipart("/api/v1/system/attachments")
                        .file(file)
                        .param("ownerType", "USER")
                        .param("ownerId", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn();
        long attachId = objectMapper.readTree(up.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        String auditorToken = accessToken(login(AUDITOR_USER, AUDITOR_PASSWORD));
        mockMvc.perform(get("/api/v1/system/attachments/" + attachId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, bearer(auditorToken)))
                .andExpect(status().isForbidden());

        MockMultipartFile evil = new MockMultipartFile("file", "../../etc/passwd", "application/pdf", new byte[]{1});
        mockMvc.perform(multipart("/api/v1/system/attachments/" + attachId + "/replace")
                        .file(evil)
                        .header(HttpHeaders.AUTHORIZATION, bearer(auditorToken)))
                .andExpect(status().isForbidden());
    }

    @Test @Order(5)
    void sensitiveValuesDoNotAppearInOperationLog() throws Exception {
        String token = accessToken(login("chain_admin", "ChainAdminTest!2026"));

        mockMvc.perform(post("/api/v1/system/users/change-password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"ChainAdminTest!2026\",\"newPassword\":\"NewPass!2026x\"}"))
                .andExpect(status().isOk());

        MvcResult audit = mockMvc.perform(get("/api/v1/system/audit-logs?page=1&pageSize=50")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        String body = audit.getResponse().getContentAsString();
        assertThat(body).doesNotContain("ChainAdminTest!2026");
        assertThat(body).doesNotContain("NewPass!2026x");
    }

    @Test @Order(6)
    void internalUserCanViewAndMarkOnlyTheirOwnMessagesRead() throws Exception {
        String token = accessToken(login(INTERNAL_USER, INTERNAL_PASSWORD));
        mockMvc.perform(get("/api/v1/messages/my")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/messages/" + internalMessageId + "/read")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/messages/" + anotherUsersMessageId + "/read")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    private MvcResult login(String username, String password) throws Exception {
        String body = objectMapper.createObjectNode()
                .put("username", username)
                .put("password", password)
                .toString();
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn();
    }

    private String accessToken(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private String bearer(String token) { return "Bearer " + token; }
}
