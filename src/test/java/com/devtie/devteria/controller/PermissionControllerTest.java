package com.devtie.devteria.controller;

import com.devtie.devteria.dto.request.PermissionRequest;
import com.devtie.devteria.dto.request.RoleRequest;
import com.devtie.devteria.dto.response.PermissionResponse;
import com.devtie.devteria.dto.response.RoleResponse;
import com.devtie.devteria.exception.AppException;
import com.devtie.devteria.exception.ErrorCode;
import com.devtie.devteria.service.PermissionService;
import com.devtie.devteria.service.RoleService;
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

import java.util.List;
import java.util.Set;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource("/test.properties")
public class PermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @MockitoBean
    private PermissionService permissionService;

    private PermissionRequest permissionRequest;
    private PermissionResponse permissionResponse;

    @BeforeEach
    void initData() {
        permissionRequest = PermissionRequest.builder()
                .name("READ_DATA")
                .description("You can read data")
                .build();
        permissionResponse = PermissionResponse.builder()
                .name("READ_DATA")
                .description("You can read data")
                .build();
    }

    //Create Role
        @Test
        @WithMockUser(username = "john123", roles = {"ADMIN"}) // Thêm roles để khớp với bảo mật
        void createPermission_validRequest_success() throws Exception {
            // Sắp xếp dữ liệu đầu vào
            ObjectMapper objectMapper = new ObjectMapper();
            String content = objectMapper.writeValueAsString(permissionRequest);

            Mockito.when(permissionService.create(ArgumentMatchers.any())).thenReturn(permissionResponse);

            mockMvc.perform(MockMvcRequestBuilders.post("/permissions")
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .content(content))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("code").value(200))
                    .andExpect(MockMvcResultMatchers.jsonPath("result.name").value("READ_DATA"))
                .andExpect(MockMvcResultMatchers.jsonPath("result.description").value("You can read data"));
    }
    //get Role
    @Test
    @WithMockUser(username = "john123")
    void getAllPermission_validRequest_success() throws Exception {
        Mockito.when(permissionService.getAll()).thenReturn(List.of(permissionResponse));

        mockMvc.perform(MockMvcRequestBuilders.get("/permissions")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(200))
                .andExpect(MockMvcResultMatchers.jsonPath("result[0].name").value("READ_DATA"))
                .andExpect(MockMvcResultMatchers.jsonPath("result[0].description").value("You can read data"));}

    // Delete role
    @Test
    @WithMockUser(username = "john123",roles = {"ADMIN"})
    void deletePermissionById_valid_success() throws Exception {
        // GIVEN
        String perId = "READ_DATA";
        Mockito.doNothing().when(permissionService).delete(perId);

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.delete("/permissions/{permission}", perId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(200));
    }
    @Test
    @WithMockUser(username = "john123",roles = {"ADMIN"})
    void deletePermissionById_valid_fail() throws Exception {
        // GIVENß
        String perId = "FOUND";
        Mockito.doThrow(new AppException(ErrorCode.PERMISSION_NOT_FOUND)).when(permissionService).delete(perId);

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.delete("/permissions/{permission}", perId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(1006))
                .andExpect(MockMvcResultMatchers.jsonPath("message").value("Permission not found"));
    }
}