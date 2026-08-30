package com.morel.greenhouse.application.service;

import com.morel.greenhouse.application.dto.BindGreenhousesRequest;
import com.morel.greenhouse.application.dto.FeedbackMessageRequest;
import com.morel.greenhouse.application.dto.FeedbackRequest;
import com.morel.greenhouse.application.dto.ProfileUpdateRequest;
import com.morel.greenhouse.application.dto.SaveUserRequest;
import com.morel.greenhouse.shared.exception.BusinessException;
import com.morel.greenhouse.shared.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAccountServiceTest {
    private JdbcTemplate jdbcTemplate;
    private PasswordEncoder passwordEncoder;
    private UserAccountService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new UserAccountService(jdbcTemplate, passwordEncoder);
    }

    @Test
    void profileReturnsUserOrThrowsNotFound() {
        when(jdbcTemplate.queryForList(anyString(), eq(7L))).thenReturn(List.of(userRow("farmer", "FARMER", false)));
        assertEquals("farmer", service.profile(7L).get("username"));

        when(jdbcTemplate.queryForList(anyString(), eq(8L))).thenReturn(List.of());
        assertEquals(404, assertThrows(BusinessException.class, () -> service.profile(8L)).getCode());
    }

    @Test
    void updateProfileKeepsExistingValuesAndRefreshesDefaultAvatar() {
        when(jdbcTemplate.queryForList(anyString(), eq(7L)))
                .thenReturn(List.of(userRow("farmer", "FARMER", false)))
                .thenReturn(List.of(userRow("newfarmer", "FARMER", false)));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("newfarmer"), eq(7L), eq(7L)))
                .thenReturn(0);

        Map<String, Object> updated = service.updateProfile(7L,
                new ProfileUpdateRequest("newfarmer", null, null, "New Farmer", "", "MALE", "bio2", false));

        assertEquals("newfarmer", updated.get("username"));
        verify(jdbcTemplate).update(anyString(), eq("newfarmer"), eq("13800000000"), eq("farmer@example.com"),
                eq("New Farmer"), eq(DefaultAvatarResolver.MALE_FARMER), eq("MALE"), eq("bio2"), eq(false), eq(7L));
    }

    @Test
    void createUserNormalizesRoleHashesPasswordAndSyncsRole() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("farmer2"), any(), any())).thenReturn(0);
        when(passwordEncoder.encode("123456")).thenReturn("encoded-default");
        when(jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE username = ?", Long.class, "farmer2"))
                .thenReturn(12L);

        Long userId = service.createUser(new SaveUserRequest("farmer2", "", "farmer", "", "", "Farmer Two", "FEMALE", "", true));

        assertEquals(12L, userId);
        verify(jdbcTemplate).update(anyString(), eq("farmer2"), eq("{bcrypt}encoded-default"), eq("FARMER"),
                eq(null), eq(null), eq("Farmer Two"), eq(DefaultAvatarResolver.FEMALE_FARMER), eq("FEMALE"),
                eq(""), eq(true));
        verify(jdbcTemplate).update("DELETE FROM auth_user_role WHERE user_id = ?", 12L);
        verify(jdbcTemplate).update(anyString(), eq(12L), eq("FARMER"));
    }

    @Test
    void createUserRejectsInvalidRoleAndDuplicateUsername() {
        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.createUser(new SaveUserRequest("bad", null, "boss", null, null, null, null, null, true))).getCode());

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("admin2"), any(), any())).thenReturn(1);
        assertEquals(409, assertThrows(BusinessException.class,
                () -> service.createUser(new SaveUserRequest("admin2", null, "ADMIN", null, null, null, null, null, true))).getCode());
    }

    @Test
    void deleteUserRejectsSelfAndProtectedAdmins() {
        CurrentUser admin = new CurrentUser(1L, "admin1", "ADMIN");
        assertEquals(400, assertThrows(BusinessException.class, () -> service.deleteUser(1L, admin)).getCode());

        when(jdbcTemplate.queryForList(anyString(), eq(2L))).thenReturn(List.of(userRow("admin1", "ADMIN", false)));
        assertEquals(400, assertThrows(BusinessException.class, () -> service.deleteUser(2L, admin)).getCode());

        when(jdbcTemplate.queryForList(anyString(), eq(3L))).thenReturn(List.of(userRow("admin2", "ADMIN", false)));
        assertEquals(400, assertThrows(BusinessException.class, () -> service.deleteUser(3L, admin)).getCode());
    }

    @Test
    void bindGreenhousesRejectsNonFarmerAndOccupiedGreenhouse() {
        when(jdbcTemplate.queryForList(anyString(), eq(3L))).thenReturn(List.of(userRow("admin2", "ADMIN", true)));
        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.bindGreenhouses(3L, new BindGreenhousesRequest(List.of(11L)), admin())).getCode());

        when(jdbcTemplate.queryForList(anyString(), eq(7L))).thenReturn(List.of(userRow("farmer", "FARMER", false)));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(11L), eq(7L))).thenReturn(1);
        assertEquals(409, assertThrows(BusinessException.class,
                () -> service.bindGreenhouses(7L, new BindGreenhousesRequest(List.of(11L)), admin())).getCode());
    }

    @Test
    void sendFeedbackMessageCreatesFarmerConversationAndStoresMessage() {
        CurrentUser farmer = new CurrentUser(7L, "farmer", "FARMER");
        when(jdbcTemplate.queryForList(anyString(), eq(7L), eq(1L))).thenReturn(List.of());
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(7L), eq(1L))).thenReturn(99L);

        service.sendFeedbackMessage(farmer, new FeedbackMessageRequest(null, 1L, null, "hello", null, null));

        verify(jdbcTemplate).update(anyString(), eq(7L), eq(1L));
        verify(jdbcTemplate).update(anyString(), eq(99L), eq(7L), eq(1L), eq("hello"), eq("TEXT"), eq(null));
        verify(jdbcTemplate).update(anyString(), eq("hello"), eq(99L));
    }

    @Test
    void feedbackMessagesRejectUnauthorizedConversation() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(99L), eq(false), eq(7L), eq(7L)))
                .thenReturn(0);

        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.feedbackMessages(new CurrentUser(7L, "farmer", "FARMER"), 99L)).getCode());
    }

    @Test
    void usersAdminsAndFarmerGreenhousesDelegateFilteredQueries() {
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(Map.of("username", "farmer")));
        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(Map.of("username", "admin1")));
        when(jdbcTemplate.queryForList(anyString(), eq(7L))).thenReturn(List.of(Map.of("id", 11L, "name", "G1")));

        assertEquals("farmer", service.users("far", "G1").get(0).get("username"));
        assertEquals("farmer", service.users("", null).get(0).get("username"));
        assertEquals("farmer", service.users(null, " ").get(0).get("username"));
        assertEquals("admin1", service.admins().get(0).get("username"));
        assertEquals(11L, service.farmerGreenhouseIds(7L).get(0).get("id"));
    }

    @Test
    void createAndUpdateUserCoverOptionalBranches() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("farmer3"), any(), any())).thenReturn(0);
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        when(jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE username = ?", Long.class, "farmer3"))
                .thenReturn(13L);

        assertEquals(13L, service.createUser(new SaveUserRequest("farmer3", "secret", null, "1", "f@example.com", "", null, null, false)));
        verify(jdbcTemplate).update(anyString(), eq("farmer3"), eq("{bcrypt}encoded-secret"), eq("FARMER"),
                eq("1"), eq("f@example.com"), eq(null), eq(DefaultAvatarResolver.MALE_FARMER), eq("MALE"),
                eq(""), eq(false));

        when(jdbcTemplate.queryForList(anyString(), eq(13L))).thenReturn(List.of(userRow("farmer3", "FARMER", false)));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("farmer3"), eq(13L), eq(13L))).thenReturn(0);
        service.updateUser(13L, new SaveUserRequest("farmer3", "", null, "", "", "", "", "", null));
        verify(jdbcTemplate).update(anyString(), eq("farmer3"), eq("FARMER"), eq(null), eq(null),
                eq(null), eq("UNKNOWN"), eq(""), eq(true), eq(13L));
    }

    @Test
    void updateUserCanChangePasswordAndDeleteAllowedAdmin() {
        when(jdbcTemplate.queryForList(anyString(), eq(3L))).thenReturn(List.of(userRow("admin3", "ADMIN", true)));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("admin3"), eq(3L), eq(3L))).thenReturn(0);
        when(passwordEncoder.encode("new-pass")).thenReturn("encoded-new");

        service.updateUser(3L, new SaveUserRequest("admin3", "new-pass", "ADMIN", "1", "a@example.com", "A", "MALE", "bio", false));
        service.deleteUser(3L, admin());

        verify(jdbcTemplate).update(anyString(), eq("{bcrypt}encoded-new"), eq("admin3"), eq("ADMIN"),
                eq("1"), eq("a@example.com"), eq("A"), eq("MALE"), eq("bio"), eq(false), eq(3L));
        verify(jdbcTemplate).update("UPDATE app_user SET deleted = TRUE, deleted_at = CURRENT_TIMESTAMP, enabled = FALSE WHERE id = ?", 3L);
        verify(jdbcTemplate).update("UPDATE farmer_greenhouse_binding SET deleted = TRUE, deleted_at = CURRENT_TIMESTAMP WHERE farmer_user_id = ?", 3L);
    }

    @Test
    void bindAndUnbindGreenhousesSuccessPath() {
        when(jdbcTemplate.queryForList(anyString(), eq(7L))).thenReturn(List.of(userRow("farmer", "FARMER", false)));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(11L), eq(7L))).thenReturn(0);
        when(jdbcTemplate.update(anyString(), eq(7L), eq(11L))).thenReturn(1);

        service.bindGreenhouses(7L, new BindGreenhousesRequest(List.of(11L)), admin());
        service.unbindGreenhouse(7L, 11L);

        verify(jdbcTemplate).update(anyString(), eq(7L), eq(11L), eq(1L));
        verify(jdbcTemplate).update(anyString(), eq(11L), eq(7L));
    }

    @Test
    void bindGreenhousesAcceptsNullListAndNullOperator() {
        when(jdbcTemplate.queryForList(anyString(), eq(7L))).thenReturn(List.of(userRow("farmer", "FARMER", false)));

        service.bindGreenhouses(7L, new BindGreenhousesRequest(null), null);

        verify(jdbcTemplate).update("UPDATE farmer_greenhouse_binding SET deleted = TRUE, deleted_at = CURRENT_TIMESTAMP WHERE farmer_user_id = ?", 7L);
        verify(jdbcTemplate).update("UPDATE greenhouse SET owner_user_id = NULL, updated_at = CURRENT_TIMESTAMP WHERE owner_user_id = ?", 7L);
    }

    @Test
    void unbindGreenhouseRejectsMissingBinding() {
        when(jdbcTemplate.queryForList(anyString(), eq(7L))).thenReturn(List.of(userRow("farmer", "FARMER", false)));
        when(jdbcTemplate.update(anyString(), eq(7L), eq(11L))).thenReturn(0);

        assertEquals(404, assertThrows(BusinessException.class, () -> service.unbindGreenhouse(7L, 11L)).getCode());
    }

    @Test
    void unbindGreenhouseRejectsNonFarmer() {
        when(jdbcTemplate.queryForList(anyString(), eq(3L))).thenReturn(List.of(userRow("admin3", "ADMIN", true)));

        assertEquals(400, assertThrows(BusinessException.class, () -> service.unbindGreenhouse(3L, 11L)).getCode());
    }

    @Test
    void feedbackAndFeedbacksUseFilters() {
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(Map.of("status", "OPEN")));

        service.feedback(new FeedbackRequest(7L, "bug", "help", "phone"));

        assertEquals("OPEN", service.feedbacks("bug", "OPEN").get(0).get("status"));
        assertEquals("OPEN", service.feedbacks("", null).get(0).get("status"));
        assertEquals("OPEN", service.feedbacks(null, " ").get(0).get("status"));
        verify(jdbcTemplate).update(anyString(), eq(7L), eq("bug"), eq("help"), eq("phone"));
    }

    @Test
    void adminFeedbackMessagesCoverConversationBranches() {
        CurrentUser admin = admin();
        Map<String, Object> conversation = Map.of("id", 99L, "admin_user_id", 1L, "farmer_user_id", 7L);
        when(jdbcTemplate.queryForList(anyString(), eq(99L))).thenReturn(List.of(conversation));

        service.sendFeedbackMessage(admin, new FeedbackMessageRequest(99L, null, null, "image ok", "image", ""));

        verify(jdbcTemplate).update(anyString(), eq(99L), eq(1L), eq(7L), eq("image ok"), eq("IMAGE"), eq(null));
        verify(jdbcTemplate).update(anyString(), org.mockito.ArgumentMatchers.contains("image ok"), eq(99L));

        when(jdbcTemplate.queryForList(anyString(), eq(98L))).thenReturn(List.of(Map.of("id", 98L, "admin_user_id", 2L, "farmer_user_id", 7L)));
        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.sendFeedbackMessage(admin, new FeedbackMessageRequest(98L, null, null, "no", null, null))).getCode());
        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.sendFeedbackMessage(admin, new FeedbackMessageRequest(null, null, null, "no", null, null))).getCode());
        when(jdbcTemplate.queryForList(anyString(), eq(97L))).thenReturn(List.of());
        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.sendFeedbackMessage(admin, new FeedbackMessageRequest(97L, null, null, "missing", null, null))).getCode());

        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(7L), eq(1L))).thenReturn(List.of(88L));
        service.sendFeedbackMessage(admin, new FeedbackMessageRequest(null, null, 7L, "direct", null, "img"));
        verify(jdbcTemplate).update(anyString(), eq(88L), eq(1L), eq(7L), eq("direct"), eq("TEXT"), eq("img"));
    }

    @Test
    void farmerFeedbackMessageRequiresSelectedAdmin() {
        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.sendFeedbackMessage(new CurrentUser(7L, "farmer", "FARMER"),
                        new FeedbackMessageRequest(null, null, null, "hello", null, null))).getCode());
    }

    @Test
    void systemMessagesConversationsMessagesAndUnreadSummary() {
        CurrentUser farmer = new CurrentUser(7L, "farmer", "FARMER");
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(7L), eq(1L))).thenReturn(List.of(66L));
        when(jdbcTemplate.queryForList(anyString(), eq(7L), eq(7L))).thenReturn(List.of(Map.of("conversation_id", 66L)));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(66L), eq(false), eq(7L), eq(7L))).thenReturn(1);
        when(jdbcTemplate.queryForList(anyString(), eq(66L))).thenReturn(List.of(Map.of("content", "hello")));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(7L))).thenReturn(2L);
        when(jdbcTemplate.queryForList(anyString(), eq(7L))).thenReturn(List.of(Map.of("conversation_id", 66L)));

        service.sendSystemMessage(7L, 1L, 1L, 7L, "sys");

        assertEquals(66L, service.feedbackConversations(farmer).get(0).get("conversation_id"));
        assertEquals("hello", service.feedbackMessages(farmer, 66L).get(0).get("content"));
        assertEquals(2L, service.unreadFeedbackSummary(farmer).get("unreadCount"));
        assertEquals(66L, service.unreadFeedbackSummary(farmer).get("firstConversationId"));
        verify(jdbcTemplate).update(anyString(), eq(66L), eq(1L), eq(7L), eq("sys"));
    }

    @Test
    void feedbackMessagesAndUnreadSummaryCoverAllowedAdminAndEmptyUnread() {
        CurrentUser admin = admin();
        when(jdbcTemplate.queryForList(anyString(), eq(1L), eq(1L), eq(1L))).thenReturn(List.of(Map.of("conversation_id", 66L)));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(66L), eq(true), eq(1L), eq(1L))).thenReturn(1);
        when(jdbcTemplate.queryForList(anyString(), eq(66L))).thenReturn(List.of(Map.of("content", "admin")));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(1L))).thenReturn(null);
        when(jdbcTemplate.queryForList(anyString(), eq(1L))).thenReturn(List.of());

        assertEquals(66L, service.feedbackConversations(admin).get(0).get("conversation_id"));
        assertEquals("admin", service.feedbackMessages(admin, 66L).get(0).get("content"));
        assertEquals(0L, service.unreadFeedbackSummary(admin).get("unreadCount"));
        assertEquals("", service.unreadFeedbackSummary(admin).get("firstConversationId"));
    }

    @Test
    void profileValidationCoversAdminNameAndCustomAvatarBranches() {
        when(jdbcTemplate.queryForList(anyString(), eq(4L)))
                .thenReturn(List.of(userRow("admin4", "ADMIN", true)))
                .thenReturn(List.of(userRow("admin4", "ADMIN", true)));
        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.updateProfile(4L, new ProfileUpdateRequest("manager", null, null, null, null, null, null, true))).getCode());

        when(jdbcTemplate.queryForList(anyString(), eq(7L)))
                .thenReturn(List.of(userRowWithAvatar("farmer", "FARMER", false, "/custom.png")))
                .thenReturn(List.of(userRowWithAvatar("farmer2", "FARMER", false, "/custom.png")));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("farmer2"), eq(7L), eq(7L))).thenReturn(0);
        service.updateProfile(7L, new ProfileUpdateRequest("farmer2", null, null, null, "/custom.png", "FEMALE", null, false));
        verify(jdbcTemplate).update(anyString(), eq("farmer2"), eq("13800000000"), eq("farmer@example.com"),
                eq("Farmer"), eq("/custom.png"), eq("FEMALE"), eq("bio"), eq(false), eq(7L));

        when(jdbcTemplate.queryForList(anyString(), eq(8L)))
                .thenReturn(List.of(userRow("farmer", "FARMER", false)));
        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.updateProfile(8L, new ProfileUpdateRequest("adminFarmer", null, null, null, null, null, null, false))).getCode());
    }

    @Test
    void privateHelpersCoverDefaultNullAndValidationBranches() throws Exception {
        var normalizeRole = UserAccountService.class.getDeclaredMethod("normalizeRole", String.class);
        normalizeRole.setAccessible(true);
        assertEquals("FARMER", normalizeRole.invoke(service, new Object[]{null}));
        assertEquals("ADMIN", normalizeRole.invoke(service, " admin "));
        assertEquals("FARMER", normalizeRole.invoke(service, "farmer"));

        var blankToDefault = UserAccountService.class.getDeclaredMethod("blankToDefault", String.class, String.class);
        blankToDefault.setAccessible(true);
        assertEquals("fallback", blankToDefault.invoke(service, new Object[]{null, "fallback"}));
        assertEquals("fallback", blankToDefault.invoke(service, " ", "fallback"));
        assertEquals("value", blankToDefault.invoke(service, "value", "fallback"));

        var emptyToNull = UserAccountService.class.getDeclaredMethod("emptyToNull", String.class);
        emptyToNull.setAccessible(true);
        assertEquals(null, emptyToNull.invoke(service, new Object[]{null}));
        assertEquals(null, emptyToNull.invoke(service, " "));
        assertEquals("x", emptyToNull.invoke(service, " x "));

        var stringValue = UserAccountService.class.getDeclaredMethod("stringValue", Object.class);
        stringValue.setAccessible(true);
        assertEquals("", stringValue.invoke(service, new Object[]{null}));
        assertEquals("12", stringValue.invoke(service, 12));

        var resolveProfileAvatar = UserAccountService.class.getDeclaredMethod(
                "resolveProfileAvatar", String.class, String.class, String.class, String.class, String.class);
        resolveProfileAvatar.setAccessible(true);
        assertEquals(DefaultAvatarResolver.MALE_FARMER,
                resolveProfileAvatar.invoke(service, new Object[]{null, DefaultAvatarResolver.FEMALE_FARMER, "FARMER", "FEMALE", "MALE"}));
        assertEquals(DefaultAvatarResolver.MALE_FARMER,
                resolveProfileAvatar.invoke(service, " ", DefaultAvatarResolver.FEMALE_FARMER, "FARMER", "FEMALE", "MALE"));
        assertEquals(DefaultAvatarResolver.MALE_FARMER,
                resolveProfileAvatar.invoke(service, DefaultAvatarResolver.FEMALE_FARMER, DefaultAvatarResolver.FEMALE_FARMER, "FARMER", "FEMALE", "MALE"));
    }

    private CurrentUser admin() {
        return new CurrentUser(1L, "admin1", "ADMIN");
    }

    private Map<String, Object> userRow(String username, String role, boolean allowAdminDelete) {
        return userRowWithAvatar(username, role, allowAdminDelete, DefaultAvatarResolver.FEMALE_FARMER);
    }

    private Map<String, Object> userRowWithAvatar(String username, String role, boolean allowAdminDelete, String avatar) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", 7L);
        row.put("username", username);
        row.put("role_code", role);
        row.put("phone", "13800000000");
        row.put("email", "farmer@example.com");
        row.put("display_name", "Farmer");
        row.put("avatar_url", avatar);
        row.put("gender", "FEMALE");
        row.put("bio", "bio");
        row.put("allow_admin_delete", allowAdminDelete);
        row.put("last_login_ip", "");
        return row;
    }
}
