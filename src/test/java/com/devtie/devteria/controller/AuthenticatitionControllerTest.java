package com.devtie.devteria.controller;

import com.devtie.devteria.dto.request.*;
import com.devtie.devteria.dto.response.AuthenticationResponse;
import com.devtie.devteria.dto.response.IntrospectResponse;
import com.devtie.devteria.dto.response.PermissionResponse;
import com.devtie.devteria.exception.AppException;
import com.devtie.devteria.exception.ErrorCode;
import com.devtie.devteria.service.AuthencationService;
import com.devtie.devteria.service.PermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource("/test.properties")
public class AuthenticatitionControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthencationService authencationService;

    private AuthenticationRequest authenticationRequest;
    private AuthenticationResponse authenticationResponse;
    private LogoutRequest logoutRequest;
    private IntrospectRequest introspectRequest;
    private IntrospectResponse introspectResponse;
    private RefreshRequest refreshRequest;

    @BeforeEach
    void initData() {
        authenticationRequest = AuthenticationRequest.builder()
                .userName("manhattan")
                .passWord("12345678")
                .build();
        authenticationResponse = AuthenticationResponse.builder()
                .authenticated(true)
                .token("eyJhbGciOiJIUzUxMiJ9")
                .build();
        logoutRequest = LogoutRequest.builder()
                .token(authenticationResponse.getToken())
                .build();
        introspectRequest = IntrospectRequest.builder()
                .token(authenticationResponse.getToken())
                .build();
        introspectResponse = IntrospectResponse.builder()
                .valid(true)
                .build();
        refreshRequest= RefreshRequest.builder()
                .token("refresh_token")
                .build();
    }
    @Test
    void login_validRequest_success() throws Exception {
        // Sắp xếp dữ liệu đầu vào
        ObjectMapper objectMapper = new ObjectMapper();
        String content = objectMapper.writeValueAsString(authenticationRequest);

        Mockito.when(authencationService.authenticate(ArgumentMatchers.any())).thenReturn(authenticationResponse);

        mockMvc.perform(MockMvcRequestBuilders.post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(200))
                .andExpect(MockMvcResultMatchers.jsonPath("result.authenticated").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("result.token").value("eyJhbGciOiJIUzUxMiJ9"));
    }

    @Test
    void login_userNameInvalid_fail() throws Exception {
        // Sắp xếp dữ liệu đầu vào
        authenticationRequest.setUserName("invalid");
        ObjectMapper objectMapper = new ObjectMapper();
        String content = objectMapper.writeValueAsString(authenticationRequest);

        Mockito.when(authencationService.authenticate(ArgumentMatchers.any())).thenThrow(new AppException(ErrorCode.USER_NOT_FOUND));

        mockMvc.perform(MockMvcRequestBuilders.post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(1002))
                .andExpect(MockMvcResultMatchers.jsonPath("message").value("User not found"));
    }
    @Test
    void login_passWordInvalid_fail() throws Exception {
        // Sắp xếp dữ liệu đầu vào
        authenticationRequest.setPassWord("11111");
        ObjectMapper objectMapper = new ObjectMapper();
        String content = objectMapper.writeValueAsString(authenticationRequest);

        Mockito.when(authencationService.authenticate(ArgumentMatchers.any())).thenThrow(new AppException(ErrorCode.USERNAME_OR_PASSWORD_NOT_MATCH));

        mockMvc.perform(MockMvcRequestBuilders.post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(1006))
                .andExpect(MockMvcResultMatchers.jsonPath("message").value("Username or password does not match"));
    }

    //Logout
    @Test
    void logout_validRequest_success() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        String content = objectMapper.writeValueAsString(logoutRequest);

        Mockito.doNothing().when(authencationService).logout(ArgumentMatchers.any());

        mockMvc.perform(MockMvcRequestBuilders.post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(200));

    }
    //Introspect
    @Test
    void introspect_validRequest_success() throws Exception {
        // Sắp xếp dữ liệu đầu vào
        ObjectMapper objectMapper = new ObjectMapper();
        String content = objectMapper.writeValueAsString(introspectRequest);

        Mockito.when(authencationService.introspect(ArgumentMatchers.any())).thenReturn(introspectResponse);

        mockMvc.perform(MockMvcRequestBuilders.post("/auth/introspect")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(200))
                .andExpect(MockMvcResultMatchers.jsonPath("result.valid").value(true));
    }

    @Test
    void introspect_validRequest_fail() throws Exception {
        // Sắp xếp dữ liệu đầu vào
        introspectResponse.setValid(false);
        ObjectMapper objectMapper = new ObjectMapper();
        String content = objectMapper.writeValueAsString(introspectRequest);

        Mockito.when(authencationService.introspect(ArgumentMatchers.any())).thenReturn(introspectResponse);

        mockMvc.perform(MockMvcRequestBuilders.post("/auth/introspect")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(200))
                .andExpect(MockMvcResultMatchers.jsonPath("result.valid").value(false));
    }

    //refresh token
    @Test
    void refresh_validRequest_success() throws Exception {
        // Sắp xếp dữ liệu đầu vào
        ObjectMapper objectMapper = new ObjectMapper();
        String content = objectMapper.writeValueAsString(refreshRequest);

        Mockito.when(authencationService.refreshToken(ArgumentMatchers.any())).thenReturn(authenticationResponse);

        mockMvc.perform(MockMvcRequestBuilders.post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(200))
                .andExpect(MockMvcResultMatchers.jsonPath("result.authenticated").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("result.token").value(authenticationResponse.getToken()));
    }

    @Test
    void refresh_validRequest_fail() throws Exception {
        // Sắp xếp dữ liệu đầu vào
        ObjectMapper objectMapper = new ObjectMapper();
        String content = objectMapper.writeValueAsString(refreshRequest);

        Mockito.when(authencationService.refreshToken(ArgumentMatchers.any())).thenThrow(new AppException(ErrorCode.UNAUTHORIZED));

        mockMvc.perform(MockMvcRequestBuilders.post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(1008))
                .andExpect(MockMvcResultMatchers.jsonPath("message").value("You do not have permission"));
    }
}
