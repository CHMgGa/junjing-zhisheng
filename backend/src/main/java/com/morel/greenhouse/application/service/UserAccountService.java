package com.morel.greenhouse.application.service;

import com.morel.greenhouse.application.dto.BindGreenhousesRequest;
import com.morel.greenhouse.application.dto.FeedbackMessageRequest;
import com.morel.greenhouse.application.dto.FeedbackRequest;
import com.morel.greenhouse.application.dto.ProfileUpdateRequest;
import com.morel.greenhouse.application.dto.SaveUserRequest;
import com.morel.greenhouse.shared.exception.BusinessException;
import com.morel.greenhouse.shared.security.CurrentUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class UserAccountService {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    public Map<String, Object> profile(Long userId) {
        return jdbcTemplate.queryForList("""
                SELECT id, username, role_code, phone, email, display_name, avatar_url, gender, bio, allow_admin_delete, last_login_ip
                FROM app_user
                WHERE id = ? AND deleted = FALSE
                """, userId).stream().findFirst().orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }

    @Transactional
    public Map<String, Object> updateProfile(Long userId, ProfileUpdateRequest request) {
        Map<String, Object> current = profile(userId);
        String role = stringValue(current.get("role_code"));
        String nextUsername = blankToDefault(request.username(), stringValue(current.get("username"))).trim();
        String oldGender = stringValue(current.get("gender"));
        String nextGender = blankToDefault(request.gender(), oldGender);
        validateUsername(nextUsername, role, userId);
        jdbcTemplate.update("""
                UPDATE app_user
                SET username = ?, phone = ?, email = ?, display_name = ?, avatar_url = ?, gender = ?, bio = ?,
                    allow_admin_delete = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND deleted = FALSE
                """,
                nextUsername,
                blankToDefault(request.phone(), stringValue(current.get("phone"))),
                blankToDefault(request.email(), stringValue(current.get("email"))),
                blankToDefault(request.displayName(), stringValue(current.get("display_name"))),
                resolveProfileAvatar(request.avatarUrl(), stringValue(current.get("avatar_url")), role, oldGender, nextGender),
                nextGender,
                blankToDefault(request.bio(), stringValue(current.get("bio"))),
                Boolean.TRUE.equals(request.allowAdminDelete()),
                userId
        );
        return profile(userId);
    }

    public List<Map<String, Object>> users(String keyword, String greenhouseName) {
        StringBuilder sql = new StringBuilder("""
                SELECT u.id, u.username, u.role_code, u.phone, u.email, u.display_name, u.avatar_url, u.gender, u.enabled,
                       u.allow_admin_delete, u.created_at, u.last_login_ip,
                       COUNT(b.greenhouse_id) AS greenhouse_count
                FROM app_user u
                LEFT JOIN farmer_greenhouse_binding b ON b.farmer_user_id = u.id AND b.deleted = FALSE
                LEFT JOIN greenhouse g ON g.id = b.greenhouse_id AND g.deleted = FALSE
                WHERE u.deleted = FALSE
                """);
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (u.username LIKE ? OR u.phone LIKE ? OR u.email LIKE ? OR u.display_name LIKE ?)");
            String pattern = "%" + keyword.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        if (greenhouseName != null && !greenhouseName.isBlank()) {
            sql.append(" AND g.name LIKE ?");
            params.add("%" + greenhouseName.trim() + "%");
        }
        sql.append("""
                GROUP BY u.id, u.username, u.role_code, u.phone, u.email, u.display_name, u.avatar_url, u.gender, u.enabled,
                         u.allow_admin_delete, u.created_at, u.last_login_ip
                ORDER BY u.created_at DESC
                """);
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public List<Map<String, Object>> admins() {
        return jdbcTemplate.queryForList("""
                SELECT id, username, display_name, avatar_url, gender, email
                FROM app_user
                WHERE role_code = 'ADMIN' AND enabled = TRUE AND deleted = FALSE
                ORDER BY username
                """);
    }

    @Transactional
    public Long createUser(SaveUserRequest request) {
        String role = normalizeRole(request.roleCode());
        String username = request.username().trim();
        validateUsername(username, role, null);
        String password = request.password() == null || request.password().isBlank() ? "123456" : request.password();
        String gender = blankToDefault(request.gender(), "MALE");
        jdbcTemplate.update("""
                INSERT INTO app_user(username, password_hash, role_code, phone, email, display_name, avatar_url, gender, bio, enabled, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'admin')
                """,
                username,
                "{bcrypt}" + passwordEncoder.encode(password),
                role,
                emptyToNull(request.phone()),
                emptyToNull(request.email()),
                emptyToNull(request.displayName()),
                DefaultAvatarResolver.resolve(role, gender),
                gender,
                blankToDefault(request.bio(), ""),
                request.enabled() == null || request.enabled()
        );
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE username = ?", Long.class, username);
        syncUserRole(userId, role);
        return userId;
    }

    @Transactional
    public void updateUser(Long userId, SaveUserRequest request) {
        Map<String, Object> current = profile(userId);
        String role = normalizeRole(request.roleCode() == null ? stringValue(current.get("role_code")) : request.roleCode());
        String username = request.username().trim();
        validateUsername(username, role, userId);
        List<Object> params = new ArrayList<>();
        String passwordSql = "";
        if (request.password() != null && !request.password().isBlank()) {
            passwordSql = " password_hash = ?,";
            params.add("{bcrypt}" + passwordEncoder.encode(request.password()));
        }
        params.add(username);
        params.add(role);
        params.add(emptyToNull(request.phone()));
        params.add(emptyToNull(request.email()));
        params.add(emptyToNull(request.displayName()));
        params.add(blankToDefault(request.gender(), "UNKNOWN"));
        params.add(blankToDefault(request.bio(), ""));
        params.add(request.enabled() == null || request.enabled());
        params.add(userId);
        jdbcTemplate.update("""
                UPDATE app_user
                SET%s username = ?, role_code = ?, phone = ?, email = ?, display_name = ?, gender = ?, bio = ?, enabled = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND deleted = FALSE
                """.formatted(passwordSql), params.toArray());
        syncUserRole(userId, role);
    }

    @Transactional
    public void deleteUser(Long userId, CurrentUser operator) {
        if (operator != null && operator.id().equals(userId)) {
            throw new BusinessException(400, "管理员不能删除自己的账号");
        }
        Map<String, Object> current = profile(userId);
        if ("admin1".equalsIgnoreCase(stringValue(current.get("username")))) {
            throw new BusinessException(400, "内置管理员账号不能删除");
        }
        if ("ADMIN".equals(stringValue(current.get("role_code"))) && !Boolean.TRUE.equals(current.get("allow_admin_delete"))) {
            throw new BusinessException(400, "该管理员未允许被其他管理员删除");
        }
        jdbcTemplate.update("UPDATE app_user SET deleted = TRUE, deleted_at = CURRENT_TIMESTAMP, enabled = FALSE WHERE id = ?", userId);
        jdbcTemplate.update("UPDATE farmer_greenhouse_binding SET deleted = TRUE, deleted_at = CURRENT_TIMESTAMP WHERE farmer_user_id = ?", userId);
    }

    @Transactional
    public void bindGreenhouses(Long farmerUserId, BindGreenhousesRequest request, CurrentUser operator) {
        Map<String, Object> farmer = profile(farmerUserId);
        if (!"FARMER".equals(stringValue(farmer.get("role_code")))) {
            throw new BusinessException(400, "只能为农户绑定大棚");
        }
        List<Long> greenhouseIds = request.greenhouseIds() == null ? List.of() : request.greenhouseIds();
        jdbcTemplate.update("UPDATE farmer_greenhouse_binding SET deleted = TRUE, deleted_at = CURRENT_TIMESTAMP WHERE farmer_user_id = ?", farmerUserId);
        jdbcTemplate.update("UPDATE greenhouse SET owner_user_id = NULL, updated_at = CURRENT_TIMESTAMP WHERE owner_user_id = ?", farmerUserId);
        for (Long greenhouseId : greenhouseIds) {
            Integer occupied = jdbcTemplate.queryForObject("""
                    SELECT COUNT(1)
                    FROM farmer_greenhouse_binding
                    WHERE greenhouse_id = ? AND farmer_user_id <> ? AND deleted = FALSE
                    """, Integer.class, greenhouseId, farmerUserId);
            if (occupied != null && occupied > 0) {
                throw new BusinessException(409, "该大棚已绑定给其他农户");
            }
            jdbcTemplate.update("""
                    INSERT INTO farmer_greenhouse_binding(farmer_user_id, greenhouse_id, assigned_by)
                    VALUES (?, ?, ?)
                    """, farmerUserId, greenhouseId, operator == null ? null : operator.id());
            jdbcTemplate.update("UPDATE greenhouse SET owner_user_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", farmerUserId, greenhouseId);
        }
    }

    public List<Map<String, Object>> farmerGreenhouseIds(Long farmerUserId) {
        return jdbcTemplate.queryForList("""
                SELECT g.id, g.name, g.location, g.status, g.area, g.crop_stage
                FROM greenhouse g
                JOIN farmer_greenhouse_binding b ON b.greenhouse_id = g.id AND b.deleted = FALSE
                WHERE b.farmer_user_id = ? AND g.deleted = FALSE
                ORDER BY g.id
                """, farmerUserId);
    }

    @Transactional
    public void unbindGreenhouse(Long farmerUserId, Long greenhouseId) {
        Map<String, Object> farmer = profile(farmerUserId);
        if (!"FARMER".equals(stringValue(farmer.get("role_code")))) {
            throw new BusinessException(400, "只能解除农户的大棚绑定");
        }
        int updated = jdbcTemplate.update("""
                UPDATE farmer_greenhouse_binding
                SET deleted = TRUE, deleted_at = CURRENT_TIMESTAMP
                WHERE farmer_user_id = ? AND greenhouse_id = ? AND deleted = FALSE
                """, farmerUserId, greenhouseId);
        if (updated == 0) {
            throw new BusinessException(404, "该农户未绑定此大棚");
        }
        jdbcTemplate.update("""
                UPDATE greenhouse
                SET owner_user_id = NULL, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND owner_user_id = ?
                """, greenhouseId, farmerUserId);
    }

    public void feedback(FeedbackRequest request) {
        jdbcTemplate.update("""
                INSERT INTO feedback(user_id, category, content, contact)
                VALUES (?, ?, ?, ?)
                """, request.userId(), request.category(), request.content(), request.contact());
    }

    public List<Map<String, Object>> feedbacks(String keyword, String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT f.*, u.username, u.display_name
                FROM feedback f
                LEFT JOIN app_user u ON u.id = f.user_id
                WHERE f.deleted = FALSE
                """);
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (f.category LIKE ? OR f.content LIKE ? OR f.contact LIKE ? OR u.username LIKE ?)");
            String pattern = "%" + keyword.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND f.status = ?");
            params.add(status.trim());
        }
        sql.append(" ORDER BY f.created_at DESC");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    @Transactional
    public Long ensureConversation(Long farmerId, Long adminId) {
        List<Long> rows = jdbcTemplate.queryForList("""
                SELECT id FROM feedback_conversation
                WHERE farmer_user_id = ? AND admin_user_id = ? AND deleted = FALSE
                """, Long.class, farmerId, adminId);
        if (!rows.isEmpty()) {
            return rows.get(0);
        }
        jdbcTemplate.update("""
                INSERT INTO feedback_conversation(farmer_user_id, admin_user_id)
                VALUES (?, ?)
                """, farmerId, adminId);
        return jdbcTemplate.queryForObject("""
                SELECT id FROM feedback_conversation
                WHERE farmer_user_id = ? AND admin_user_id = ? AND deleted = FALSE
                """, Long.class, farmerId, adminId);
    }

    @Transactional
    public void sendFeedbackMessage(CurrentUser sender, FeedbackMessageRequest request) {
        Long conversationId;
        Long receiverId;
        if (sender.admin()) {
            if (request.conversationId() != null) {
                conversationId = request.conversationId();
                Map<String, Object> conversation = conversation(conversationId);
                Long adminId = Long.valueOf(String.valueOf(conversation.get("admin_user_id")));
                if (!adminId.equals(sender.id())) {
                    throw new BusinessException(403, "只能在自己的管理员会话中回复");
                }
                receiverId = Long.valueOf(String.valueOf(conversation.get("farmer_user_id")));
            } else if (request.receiverUserId() != null) {
                receiverId = request.receiverUserId();
                conversationId = ensureConversation(receiverId, sender.id());
            } else {
                throw new BusinessException(400, "管理员发送消息需要选择农户会话");
            }
        } else {
            if (request.adminUserId() == null) {
                throw new BusinessException(400, "请选择管理员");
            }
            conversationId = ensureConversation(sender.id(), request.adminUserId());
            receiverId = request.adminUserId();
        }
        String messageType = request.messageType() == null || request.messageType().isBlank() ? "TEXT" : request.messageType().trim().toUpperCase();
        jdbcTemplate.update("""
                INSERT INTO feedback_message(conversation_id, sender_user_id, receiver_user_id, content, message_type, image_url)
                VALUES (?, ?, ?, ?, ?, ?)
                """, conversationId, sender.id(), receiverId, request.content().trim(), messageType, emptyToNull(request.imageUrl()));
        jdbcTemplate.update("""
                UPDATE feedback_conversation
                SET last_message = ?, last_message_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, "IMAGE".equals(messageType) ? "[图片] " + request.content().trim() : request.content().trim(), conversationId);
    }

    @Transactional
    public void sendSystemMessage(Long farmerId, Long adminId, Long senderId, Long receiverId, String content) {
        Long conversationId = ensureConversation(farmerId, adminId);
        jdbcTemplate.update("""
                INSERT INTO feedback_message(conversation_id, sender_user_id, receiver_user_id, content, message_type)
                VALUES (?, ?, ?, ?, 'TEXT')
                """, conversationId, senderId, receiverId, content);
        jdbcTemplate.update("""
                UPDATE feedback_conversation
                SET last_message = ?, last_message_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, content, conversationId);
    }

    private Map<String, Object> conversation(Long conversationId) {
        return jdbcTemplate.queryForList("""
                SELECT *
                FROM feedback_conversation
                WHERE id = ? AND deleted = FALSE
                """, conversationId).stream().findFirst().orElseThrow(() -> new BusinessException(404, "反馈会话不存在"));
    }

    public List<Map<String, Object>> feedbackConversations(CurrentUser currentUser) {
        if (currentUser.admin()) {
            return jdbcTemplate.queryForList("""
                    SELECT c.id AS conversation_id, c.status, c.last_message, c.last_message_at, c.created_at,
                           f.id AS farmer_user_id,
                           f.username AS farmer_username, f.display_name AS farmer_name, f.avatar_url AS farmer_avatar_url, f.gender AS farmer_gender,
                           a.id AS admin_user_id, a.username AS admin_username, a.display_name AS admin_name, a.avatar_url AS admin_avatar_url, a.gender AS admin_gender,
                           COALESCE((
                               SELECT COUNT(1)
                               FROM feedback_message m
                               WHERE m.conversation_id = c.id
                                 AND m.receiver_user_id = ?
                                 AND m.read_at IS NULL
                                 AND m.deleted = FALSE
                           ), 0) AS unread_count
                    FROM app_user f
                    LEFT JOIN feedback_conversation c ON c.farmer_user_id = f.id AND c.admin_user_id = ? AND c.deleted = FALSE
                    JOIN app_user a ON a.id = ?
                    WHERE f.role_code = 'FARMER' AND f.enabled = TRUE AND f.deleted = FALSE
                    ORDER BY c.last_message_at IS NULL, c.last_message_at DESC, f.username
                    """, currentUser.id(), currentUser.id(), currentUser.id());
        }
        return jdbcTemplate.queryForList("""
                SELECT a.id AS admin_user_id, a.username, a.display_name, a.avatar_url, a.gender,
                       c.id AS conversation_id, c.last_message, c.last_message_at,
                       COALESCE((
                           SELECT COUNT(1)
                           FROM feedback_message m
                           WHERE m.conversation_id = c.id
                             AND m.receiver_user_id = ?
                             AND m.read_at IS NULL
                             AND m.deleted = FALSE
                       ), 0) AS unread_count
                FROM app_user a
                LEFT JOIN feedback_conversation c ON c.admin_user_id = a.id AND c.farmer_user_id = ? AND c.deleted = FALSE
                WHERE a.role_code = 'ADMIN' AND a.enabled = TRUE AND a.deleted = FALSE
                ORDER BY c.last_message_at IS NULL, c.last_message_at DESC, a.username
                """, currentUser.id(), currentUser.id());
    }

    @Transactional
    public List<Map<String, Object>> feedbackMessages(CurrentUser currentUser, Long conversationId) {
        Integer allowed = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM feedback_conversation
                WHERE id = ?
                  AND deleted = FALSE
                  AND (? = TRUE OR farmer_user_id = ? OR admin_user_id = ?)
                """, Integer.class, conversationId, currentUser.admin(), currentUser.id(), currentUser.id());
        if (allowed == null || allowed == 0) {
            throw new BusinessException(403, "无权查看该反馈会话");
        }
        jdbcTemplate.update("""
                UPDATE feedback_message
                SET read_at = CURRENT_TIMESTAMP
                WHERE conversation_id = ? AND receiver_user_id = ? AND read_at IS NULL AND deleted = FALSE
                """, conversationId, currentUser.id());
        return jdbcTemplate.queryForList("""
                SELECT m.*, u.username, u.display_name, u.avatar_url, u.gender
                FROM feedback_message m
                JOIN app_user u ON u.id = m.sender_user_id
                WHERE m.conversation_id = ? AND m.deleted = FALSE
                ORDER BY m.created_at
                """, conversationId);
    }

    public Map<String, Object> unreadFeedbackSummary(CurrentUser currentUser) {
        Long total = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM feedback_message
                WHERE receiver_user_id = ? AND read_at IS NULL AND deleted = FALSE
                """, Long.class, currentUser.id());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT conversation_id
                FROM feedback_message
                WHERE receiver_user_id = ? AND read_at IS NULL AND deleted = FALSE
                ORDER BY created_at
                LIMIT 1
                """, currentUser.id());
        return Map.of(
                "unreadCount", total == null ? 0 : total,
                "firstConversationId", rows.isEmpty() ? "" : rows.get(0).get("conversation_id")
        );
    }

    private void validateUsername(String username, String role, Long currentUserId) {
        String normalizedName = username == null ? "" : username.trim();
        if ("ADMIN".equals(role) && !normalizedName.toLowerCase().startsWith("admin")) {
            throw new BusinessException(400, "管理员用户名必须以 admin 开头");
        }
        if (!"ADMIN".equals(role) && normalizedName.toLowerCase().startsWith("admin")) {
            throw new BusinessException(400, "农户用户名不能以 admin 开头");
        }
        Integer exists = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM app_user
                WHERE username = ?
                  AND deleted = FALSE
                  AND (? IS NULL OR id <> ?)
                """, Integer.class, normalizedName, currentUserId, currentUserId);
        if (exists != null && exists > 0) {
            throw new BusinessException(409, "用户名已存在");
        }
    }

    private void syncUserRole(Long userId, String role) {
        jdbcTemplate.update("DELETE FROM auth_user_role WHERE user_id = ?", userId);
        jdbcTemplate.update("""
                INSERT INTO auth_user_role(user_id, role_id)
                SELECT ?, id FROM auth_role WHERE role_code = ?
                """, userId, role);
    }

    private String normalizeRole(String role) {
        String normalized = role == null ? "FARMER" : role.trim().toUpperCase();
        if (!"ADMIN".equals(normalized) && !"FARMER".equals(normalized)) {
            throw new BusinessException(400, "角色只能是管理员或农户");
        }
        return normalized;
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String resolveProfileAvatar(String requestedAvatar, String currentAvatar, String role, String oldGender, String nextGender) {
        String oldDefault = DefaultAvatarResolver.resolve(role, oldGender);
        if (requestedAvatar == null || requestedAvatar.isBlank() || requestedAvatar.equals(oldDefault)) {
            return DefaultAvatarResolver.resolve(role, nextGender);
        }
        return requestedAvatar;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
