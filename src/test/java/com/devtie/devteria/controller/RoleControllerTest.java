package com.devtie.devteria.controller;

import java.util.List;
import java.util.Set;

import com.devtie.devteria.dto.response.PermissionResponse;
import com.devtie.devteria.entity.User;
import com.devtie.devteria.exception.AppException;
import com.devtie.devteria.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.devtie.devteria.dto.request.RoleRequest;
import com.devtie.devteria.dto.response.RoleResponse;
import com.devtie.devteria.service.RoleService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import static org.mockito.ArgumentMatchers.any;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource("/test.properties")
public class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoleService roleService;

    private RoleRequest roleRequest;
    private RoleResponse roleResponse;

    @BeforeEach
    void initData() {
        roleRequest = RoleRequest.builder()
                .name("USER")
                .description("User role")
                .permissions(Set.of("READ_POST"))
                .build();
        roleResponse = RoleResponse.builder()
                .name("USER")
                .description("User role")
                .permissions(Set.of(
                        PermissionResponse.builder()
                                .name("READ_POST")
                                .description("Read post permission")
                                .build()
                ))
                .build();
    }

    //Create Role
        @Test
        @WithMockUser(username = "john123", roles = {"ADMIN"}) // Thêm roles để khớp với bảo mật
        void createRole_validRequest_success() throws Exception {
            // Sắp xếp dữ liệu đầu vào
            ObjectMapper objectMapper = new ObjectMapper();
            String content = objectMapper.writeValueAsString(roleRequest);

            Mockito.when(roleService.create(ArgumentMatchers.any(RoleRequest.class))).thenReturn(roleResponse);

            mockMvc.perform(MockMvcRequestBuilders.post("/roles")
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .content(content))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("code").value(200))
                    .andExpect(MockMvcResultMatchers.jsonPath("result.name").value("USER"))
                .andExpect(MockMvcResultMatchers.jsonPath("result.description").value("User role"))
                .andExpect(MockMvcResultMatchers.jsonPath("result.permissions[0].name").value("READ_POST"));
    }
    //get Role
    @Test
    @WithMockUser(username = "john123")
    void getRole_validRequest_success() throws Exception {
        Mockito.when(roleService.getRoleAll()).thenReturn(List.of(roleResponse));

        mockMvc.perform(MockMvcRequestBuilders.get("/roles")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(200))
                .andExpect(MockMvcResultMatchers.jsonPath("result[0].name").value("USER"))
                .andExpect(MockMvcResultMatchers.jsonPath("result[0].description").value("User role"));
    }

    // Delete role
    @Test
    @WithMockUser(username = "john123",roles = {"ADMIN"})
    void deleteRoleById_valid_success() throws Exception {
        // GIVEN
        String roleId = "USER";
        Mockito.doNothing().when(roleService).deleteRole(roleId);

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.delete("/roles/{roleId}", roleId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(200));
    }
    @Test
    @WithMockUser(username = "john123",roles = {"ADMIN"})
    void deleteRoleById_valid_fail() throws Exception {
        // GIVENß
        String roleId = "FOUND";
        Mockito.doThrow(new AppException(ErrorCode.ROLE_NOT_FOUND))
                .when(roleService).deleteRole(roleId);

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.delete("/roles/{roleId}", roleId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(1005))
                .andExpect(MockMvcResultMatchers.jsonPath("message").value("Role not found"));
    }
}