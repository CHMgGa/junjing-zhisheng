package com.morel.greenhouse.interfaces.controller;

import com.morel.greenhouse.application.service.AuthService;
import com.morel.greenhouse.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
class AuthControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @Test
    void policyReturnsPrivacyDocument() throws Exception {
        mockMvc.perform(get("/api/v1/auth/policies/privacy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.title", notNullValue()))
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }

    @Test
    void policyReturnsNotFoundForUnknownType() throws Exception {
        mockMvc.perform(get("/api/v1/auth/policies/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }
}
