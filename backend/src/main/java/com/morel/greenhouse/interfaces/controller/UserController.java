package com.morel.greenhouse.interfaces.controller;

import com.morel.greenhouse.application.dto.BindGreenhousesRequest;
import com.morel.greenhouse.application.dto.FeedbackRequest;
import com.morel.greenhouse.application.dto.FeedbackMessageRequest;
import com.morel.greenhouse.application.dto.ProfileUpdateRequest;
import com.morel.greenhouse.application.dto.SaveUserRequest;
import com.morel.greenhouse.application.service.UserAccountService;
import com.morel.greenhouse.shared.api.ApiResult;
import com.morel.greenhouse.shared.security.ApiSecurityInterceptor;
import com.morel.greenhouse.shared.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserAccountService userAccountService;

    public UserController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping("/{id}/profile")
    public ApiResult<Map<String, Object>> profile(@PathVariable Long id, HttpServletRequest request) {
        Map<String, Object> profile = userAccountService.profile(id);
        profile.put("realtimeIp", clientIp(request));
        return ApiResult.ok(profile);
    }

    @PutMapping("/{id}/profile")
    public ApiResult<Map<String, Object>> updateProfile(@PathVariable Long id, @RequestBody ProfileUpdateRequest request) {
        return ApiResult.ok(userAccountService.updateProfile(id, request));
    }

    @PostMapping("/feedback")
    public ApiResult<Void> feedback(@Valid @RequestBody FeedbackRequest request) {
        userAccountService.feedback(request);
        return ApiResult.ok();
    }

    @GetMapping
    public ApiResult<List<Map<String, Object>>> users(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String greenhouse
    ) {
        return ApiResult.ok(userAccountService.users(keyword, greenhouse));
    }

    @PostMapping
    public ApiResult<Map<String, Object>> createUser(@Valid @RequestBody SaveUserRequest request) {
        Long id = userAccountService.createUser(request);
        return ApiResult.ok(Map.of("id", id));
    }

    @PutMapping("/{id}")
    public ApiResult<Void> updateUser(@PathVariable Long id, @Valid @RequestBody SaveUserRequest request) {
        userAccountService.updateUser(id, request);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        userAccountService.deleteUser(id, currentUser(request));
        return ApiResult.ok();
    }

    @GetMapping("/admins")
    public ApiResult<List<Map<String, Object>>> admins() {
        return ApiResult.ok(userAccountService.admins());
    }

    @GetMapping("/{id}/greenhouses")
    public ApiResult<List<Map<String, Object>>> farmerGreenhouses(@PathVariable Long id) {
        return ApiResult.ok(userAccountService.farmerGreenhouseIds(id));
    }

    @PostMapping("/{id}/greenhouses")
    public ApiResult<Void> bindGreenhouses(@PathVariable Long id, @RequestBody BindGreenhousesRequest request, HttpServletRequest servletRequest) {
        userAccountService.bindGreenhouses(id, request, currentUser(servletRequest));
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}/greenhouses/{greenhouseId}")
    public ApiResult<Void> unbindGreenhouse(@PathVariable Long id, @PathVariable Long greenhouseId) {
        userAccountService.unbindGreenhouse(id, greenhouseId);
        return ApiResult.ok();
    }

    @GetMapping("/feedback")
    public ApiResult<List<Map<String, Object>>> feedbacks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        return ApiResult.ok(userAccountService.feedbacks(keyword, status));
    }

    @GetMapping("/feedback/conversations")
    public ApiResult<List<Map<String, Object>>> feedbackConversations(HttpServletRequest request) {
        return ApiResult.ok(userAccountService.feedbackConversations(currentUser(request)));
    }

    @GetMapping("/feedback/unread")
    public ApiResult<Map<String, Object>> unreadFeedback(HttpServletRequest request) {
        return ApiResult.ok(userAccountService.unreadFeedbackSummary(currentUser(request)));
    }

    @GetMapping("/feedback/conversations/{id}/messages")
    public ApiResult<List<Map<String, Object>>> feedbackMessages(@PathVariable Long id, HttpServletRequest request) {
        return ApiResult.ok(userAccountService.feedbackMessages(currentUser(request), id));
    }

    @PostMapping("/feedback/messages")
    public ApiResult<Void> sendFeedbackMessage(@Valid @RequestBody FeedbackMessageRequest request, HttpServletRequest servletRequest) {
        userAccountService.sendFeedbackMessage(currentUser(servletRequest), request);
        return ApiResult.ok();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        return (CurrentUser) request.getAttribute(ApiSecurityInterceptor.CURRENT_USER_ATTRIBUTE);
    }
}
