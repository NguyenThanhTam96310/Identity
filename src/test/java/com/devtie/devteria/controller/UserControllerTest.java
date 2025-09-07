package com.devtie.devteria.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

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

import com.devtie.devteria.dto.request.UserCreationRequest;
import com.devtie.devteria.dto.request.UserUpdateRequest;
import com.devtie.devteria.dto.response.UserResponse;
import com.devtie.devteria.entity.Role;
import com.devtie.devteria.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource("/test.properties")
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private UserCreationRequest request;
    private UserUpdateRequest updateRequest;
    private UserResponse userResponse;
    private LocalDate dob;
    private Role role;

    @BeforeEach // chạy phương thức này đầu tiên trc khi chạy test khác
    void initData() {
        dob = LocalDate.of(1990, 1, 1);
        request = UserCreationRequest.builder()
                .userName("john123")
                .firstName("John")
                .lastName("Doe")
                .passWord("12345678")
                .dob(dob)
                .build();
        updateRequest = UserUpdateRequest.builder()
                .firstName("John123")
                .lastName("Doe")
                .passWord("12345678")
                .dob(dob)
                .roles(List.of("USER"))
                .build();
        updateRequest = UserUpdateRequest.builder()
                .firstName("John123")
                .lastName("Doe")
                .passWord("12345678")
                .dob(dob)
                .roles(List.of("USER"))
                .build();
        role = Role.builder().name("USER").description("User role").build();
        userResponse = UserResponse.builder()
                .id("de2b8428-15fe-49cd-82c3")
                .userName("john123")
                .firstName("John")
                .lastName("Doe")
                .dob(dob)
                .roles(Set.of(role))
                .build();
    }
    // CreateUser Test
    @Test
    void createUser_validRequest_success() throws Exception {
        // GIVEN nhứng dl đầu vào và dự doán trc
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String content = objectMapper.writeValueAsString(request);
        // mockito hàm create qua từ service
        Mockito.when(userService.createUser(ArgumentMatchers.any())).thenReturn(userResponse);
        // WHEN khi nào chúng ta test, THEN
        mockMvc.perform(MockMvcRequestBuilders.post("/users")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(200))
                .andExpect(MockMvcResultMatchers.jsonPath("result.id").value("de2b8428-15fe-49cd-82c3"))
                .andExpect(MockMvcResultMatchers.jsonPath("result.userName").value("john123"))
                .andExpect(MockMvcResultMatchers.jsonPath("result.firstName").value("John"))
                .andExpect(MockMvcResultMatchers.jsonPath("result.lastName").value("Doe"));
    }

    @Test
    void createUser_usernameInvalid_fail() throws Exception {
        // GIVEN nhứng dl đầu vào và dự doán trc
        request.setUserName("jo");

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String content = objectMapper.writeValueAsString(request);
        // WHEN khi nào chúng ta test, THEN
        mockMvc.perform(MockMvcRequestBuilders.post("/users")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(2003))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("message").value("Username must be at least 3 characters long"));
    }

    @Test
    void createUser_passwordInvalid_fail() throws Exception {
        // GIVEN
        request.setPassWord("1234567");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String content = objectMapper.writeValueAsString(request);

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.post("/users")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(2004))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("message").value("Password must be at least 8 characters long"));
    }

    @Test
    void createUser_firstNameInvalid_fail() throws Exception {
        // GIVEN
        request.setFirstName("T");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String content = objectMapper.writeValueAsString(request);

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.post("/users")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(2005))
                .andExpect(MockMvcResultMatchers.jsonPath("message")
                        .value("First name must be at least 2 characters long"));
    }

    @Test
    void createUser_lastNameInvalid_fail() throws Exception {
        // GIVEN
        request.setLastName("N");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String content = objectMapper.writeValueAsString(request);

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.post("/users")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(2006))
                .andExpect(MockMvcResultMatchers.jsonPath("message")
                        .value("Last name must be at least 2 characters long"));
    }

    @Test
    void createUser_dobInvalid_fail() throws Exception {
        // GIVEN
        dob = LocalDate.of(2020, 1, 1);
        request.setDob(dob);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String content = objectMapper.writeValueAsString(request);

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.post("/users")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(2002))
                .andExpect(MockMvcResultMatchers.jsonPath("message").value("Your age must be at least 16"));
    }

    // UpdateUser Test
    @Test
    @WithMockUser(
            username = "john123",
            roles = {"USER"})
    void updateUser_validRequest_success() throws Exception {
        // GIVEN nhứng dl đầu vào và dự doán trc
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String content = objectMapper.writeValueAsString(updateRequest);
        // mockito hàm create qua từ service
        Mockito.when(userService.updateUser(Mockito.eq("de2b8428-15fe-49cd-82c3"), ArgumentMatchers.any()))
                .thenReturn(userResponse);
        // WHEN khi nào chúng ta test, THEN
        mockMvc.perform(MockMvcRequestBuilders.put("/users/{userId}", "de2b8428-15fe-49cd-82c3")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(200))
                .andExpect(MockMvcResultMatchers.jsonPath("result.id").value("de2b8428-15fe-49cd-82c3"))
                .andExpect(MockMvcResultMatchers.jsonPath("result.userName").value("john123"))
                .andExpect(MockMvcResultMatchers.jsonPath("result.firstName").value("John"))
                .andExpect(MockMvcResultMatchers.jsonPath("result.lastName").value("Doe"))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("result.roles[0].name").value("USER"));
    }

    @Test
    @WithMockUser(
            username = "john123",
            roles = {"USER"})
    void updateUser_passwordInvalid_fail() throws Exception {
        // GIVEN
        updateRequest.setPassWord("123456");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String content = objectMapper.writeValueAsString(updateRequest);

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.put("/users/{userId}", "de2b8428-15fe-49cd-82c3")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(2004))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("message").value("Password must be at least 8 characters long"));
    }

    @Test
    @WithMockUser(
            username = "john123",
            roles = {"USER"})
    void updateUser_firstNameInvalid_fail() throws Exception {
        // GIVEN
        updateRequest.setFirstName("T");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String content = objectMapper.writeValueAsString(updateRequest);

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.put("/users/{userId}", "de2b8428-15fe-49cd-82c3")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(2005))
                .andExpect(MockMvcResultMatchers.jsonPath("message")
                        .value("First name must be at least 2 characters long"));
    }

    @Test
    @WithMockUser(
            username = "john123",
            roles = {"USER"})
    void updateUser_lastNameInvalid_fail() throws Exception {
        // GIVEN
        updateRequest.setLastName("T");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String content = objectMapper.writeValueAsString(updateRequest);

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.put("/users/{userId}", "de2b8428-15fe-49cd-82c3")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(2006))
                .andExpect(MockMvcResultMatchers.jsonPath("message")
                        .value("Last name must be at least 2 characters long"));
    }

    @Test
    @WithMockUser(
            username = "john123",
            roles = {"USER"})
    void updateUser_dobInvalid_fail() throws Exception {
        // GIVEN
        dob = LocalDate.of(2020, 1, 1);
        updateRequest.setDob(dob);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String content = objectMapper.writeValueAsString(updateRequest);

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.put("/users/{userId}", "de2b8428-15fe-49cd-82c3")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(2002))
                .andExpect(MockMvcResultMatchers.jsonPath("message").value("Your age must be at least 16"));
    }


    //get All Users
    @Test
    @WithMockUser(
            username = "john123",
            roles = {"ADMIN"})
    void getAllUser_valid_success() throws Exception {
        // GIVEN
        Mockito.when(userService.getUsers())
                .thenReturn(List.of(userResponse));

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.get("/users")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(200))
                .andExpect(MockMvcResultMatchers.jsonPath("result[0].id").value("de2b8428-15fe-49cd-82c3"))
                .andExpect(MockMvcResultMatchers.jsonPath("result[0].userName").value("john123"))
                .andExpect(MockMvcResultMatchers.jsonPath("result[0].firstName").value("John"))
                .andExpect(MockMvcResultMatchers.jsonPath("result[0].lastName").value("Doe"))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("result[0].roles[0].name").value("USER"));
    }

    @Test
    void getAllUser_unauthorized_fail() throws Exception {
        // GIVEN
        Mockito.when(userService.getUsers())
                .thenReturn(List.of(userResponse));

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.get("/users")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(1007))
                .andExpect(MockMvcResultMatchers.jsonPath("message").value("Unauthenticated"));
    }


    //Get User By Id
    @Test
    @WithMockUser( username = "john123")
    void getUserById_valid_success() throws Exception {
        // GIVEN
        dob = LocalDate.of(1990, 1, 1);
        Mockito.when(userService.getUserById(Mockito.eq("de2b8428-15fe-49cd-82c3")))
                .thenReturn(userResponse);

        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.get("/users/{userId}", "de2b8428-15fe-49cd-82c3")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(200))
                .andExpect(MockMvcResultMatchers.jsonPath("result.id").value("de2b8428-15fe-49cd-82c3"))
                .andExpect(MockMvcResultMatchers.jsonPath("result.userName").value("john123"))
                .andExpect(MockMvcResultMatchers.jsonPath("result.firstName").value("John"))
                .andExpect(MockMvcResultMatchers.jsonPath("result.dob").value("1990-01-01"))
                .andExpect(MockMvcResultMatchers.jsonPath("result.lastName").value("Doe"))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("result.roles[0].name").value("USER"));
    }

    //Delete User By Id
    @Test
    @WithMockUser(username = "john123")
    void deleteUserById_valid_success() throws Exception {
        // GIVEN
        String userId = "de2b8428-15fe-49cd-82c3";
        String successMessage = "User with id " + userId + " deleted successfully";
        Mockito.doNothing().when(userService).deleteUser(Mockito.eq(userId));
        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.delete("/users/{userId}", "de2b8428-15fe-49cd-82c3")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(successMessage));}

    //Delete User By Id
    @Test
    void deleteUserById_unauthenticated_fail() throws Exception {
        // GIVEN
        String userId = "de2b8428-15fe-49cd-82c3";
        Mockito.doNothing().when(userService).deleteUser(Mockito.eq(userId));
        // WHEN, THEN
        mockMvc.perform(MockMvcRequestBuilders.delete("/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(1007))
                .andExpect(MockMvcResultMatchers.jsonPath("message").value("Unauthenticated"));
    }
}
