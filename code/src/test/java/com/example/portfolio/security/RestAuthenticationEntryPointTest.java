package com.example.portfolio.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

class RestAuthenticationEntryPointTest {

    @Test
    @DisplayName("ไม่มี token → 401 เป็น JSON รูปแบบเดียวกับ ErrorResponse")
    void writesErrorResponseJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/portfolios");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAuthenticationEntryPoint().commence(request, response,
                new InsufficientAuthenticationException("no token"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString())
                .contains("\"status\":401")
                .contains("\"error\":\"Unauthorized\"")
                .contains("\"path\":\"/api/v1/portfolios\"")
                .contains("\"details\":[]");
    }
}
