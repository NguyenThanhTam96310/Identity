package com.devtie.devteria.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.devtie.devteria.constant.PredefinedRole;
import com.devtie.devteria.dto.request.UserUpdateRequest;
import com.devtie.devteria.entity.Role;
import com.devtie.devteria.repository.RoleRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.devtie.devteria.dto.request.UserCreationRequest;
import com.devtie.devteria.dto.response.UserResponse;
import com.devtie.devteria.entity.User;
import com.devtie.devteria.exception.AppException;
import com.devtie.devteria.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource("/test.properties")
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private PasswordEncoder encoder;

    private UserCreationRequest request;
    private UserResponse userResponse;
    private UserUpdateRequest updateRequest;
    private User user;
    private LocalDate dob;
    private Role userRole;


    @BeforeEach
        // chạy phương thức này đầu tiên trc khi chạy test kh
    void initData() {
        dob = LocalDate.of(1990, 1, 1);
        request = UserCreationRequest.builder()
                .userName("john123")
                .firstName("John")
                .lastName("Doe")
                .passWord("12345678")
                .dob(dob)
                .build();
        userResponse = UserResponse.builder()
                .id("de2b8428-15fe-49cd-82c3")
                .userName("john123")
                .firstName("John")
                .lastName("Doe")
                .dob(dob)
                .build();
        user = User.builder()
                .userName("john123")
                .id("de2b8428-15fe-49cd-82c3")
                .firstName("John")
                .lastName("Doe")
                .dob(dob)
                .build();
        userRole = Role.builder()
                .name("USER")
                .description("User role")
                .build();
        updateRequest = UserUpdateRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .passWord("newpassword")
                .dob(dob)
                .roles(List.of(PredefinedRole.USER_ROLE))
                .build();
    }

    @Test
    void createUser_validRequest_success() {
        // Give
        Mockito.when(roleRepository.findById(PredefinedRole.USER_ROLE))
                .thenReturn(Optional.of(userRole));
        Mockito.when(userRepository.existsByUserName(anyString())).thenReturn(false);
        Mockito.when(userRepository.save(any())).thenReturn(user);
        Mockito.when(encoder.encode("12345678")).thenReturn("encodedPassword");

        // When
        var response = userService.createUser(request);

        // Then
        Mockito.verify(encoder).encode("12345678");
        Assertions.assertThat(response.getId()).isEqualTo("de2b8428-15fe-49cd-82c3");
        Assertions.assertThat(response.getUserName()).isEqualTo("john123");
        Assertions.assertThat(response.getFirstName()).isEqualTo("John");
        Assertions.assertThat(response.getDob()).isEqualTo("1990-01-01");
        Assertions.assertThat(response.getLastName()).isEqualTo("Doe");

    }

    @Test
    void createUser_userExisted_error() {
        // Given
        //        Mockito.when(userRepository.existsByUserName(anyString())).thenReturn(true);
        Mockito.when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Username already exists"));

        // When
        var exception = assertThrows(AppException.class, () -> userService.createUser(request));

        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1001);
        Assertions.assertThat(exception.getErrorCode().getMessage()).isEqualTo("Username already exists");
    }

    //getMyInfo
    @Test
    @WithMockUser(username = "john123")
    // có thể hash role vào
    void getMyInfo_valid_succuss() {
        // mock repository qua
        Mockito.when(userRepository.findByUserName(anyString())).thenReturn(Optional.of(user));

        // when
        var res = userService.getMyInfo();
        Assertions.assertThat(res.getUserName()).isEqualTo("john123");
        Assertions.assertThat(res.getId()).isEqualTo("de2b8428-15fe-49cd-82c3");
    }

    @Test
    @WithMockUser(username = "john123")
        // có thể hash role vào
    void getMyInfo_userNotFound_error() {
        // mock repository qua
        Mockito.when(userRepository.findByUserName(anyString())).thenReturn(Optional.ofNullable(null));

        // when
        var exception = assertThrows(AppException.class, () -> userService.getMyInfo());
        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1002);
    }


    //getAllUsers
    @Test
    @WithMockUser(username = "john", roles = {"ADMIN"})
    void getUsers_valid_succuss() {
        // given
        user.setRoles(Set.of(userRole));

        Mockito.when(userRepository.findAll()).thenReturn(List.of(user));

        // when
        var res = userService.getUsers();

        // then
        Assertions.assertThat(res).hasSize(1);
        var firstUser = res.getFirst();
        Assertions.assertThat(firstUser.getUserName()).isEqualTo("john123");
        Assertions.assertThat(firstUser.getId()).isEqualTo("de2b8428-15fe-49cd-82c3");
        Assertions.assertThat(firstUser.getFirstName()).isEqualTo("John");
        Assertions.assertThat(firstUser.getLastName()).isEqualTo("Doe");
        Assertions.assertThat(firstUser.getDob()).isEqualTo("1990-01-01");

        // check roles
        Assertions.assertThat(firstUser.getRoles())
                .extracting("name", String.class)
                .contains("USER");

        Assertions.assertThat(firstUser.getRoles())
                .extracting("description", String.class)
                .contains("User role");

    }

    //getUserById
    @Test
    @WithMockUser(username = "john123")
    // có thể hash role vào
    void getUserById_valid_succuss() {
        // mock repository qua
        user.setRoles(Set.of(userRole));
        Mockito.when(userRepository.findById("de2b8428-15fe-49cd-82c3")).thenReturn(Optional.of(user));
        // when
        var res = userService.getUserById("de2b8428-15fe-49cd-82c3");
        Assertions.assertThat(res.getUserName()).isEqualTo("john123");
        Assertions.assertThat(res.getId()).isEqualTo("de2b8428-15fe-49cd-82c3");
        Assertions.assertThat(res.getFirstName()).isEqualTo("John");
        Assertions.assertThat(res.getLastName()).isEqualTo("Doe");
        Assertions.assertThat(res.getDob()).isEqualTo("1990-01-01");

        // check roles
        Assertions.assertThat(res.getRoles())
                .extracting("name", String.class)
                .contains("USER");

        Assertions.assertThat(res.getRoles())
                .extracting("description", String.class)
                .contains("User role");
    }

    @Test
    @WithMockUser(username = "john123")
    void getUserById_userNotFound_error() {
        // mock repository qua
        var userId = "de2b8428-15fe-49cd-82c3";
        user.setRoles(Set.of(userRole));
        Mockito.when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When
        var exception = assertThrows(RuntimeException.class, () -> userService.getUserById(userId));

        // Then
        Assertions.assertThat(exception.getMessage())
                .isEqualTo("User not found with id: " + userId);
    }


    //Update user service
    @Test
    void undateUser_validRequest_success() {
        // Give
        var userId = "de2b8428-15fe-49cd-82c3";
        user.setRoles(Set.of(userRole));
        Mockito.when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        Mockito.when(roleRepository.findAllById(List.of(PredefinedRole.USER_ROLE)))
                .thenReturn(List.of(userRole));
        Mockito.when(userRepository.save(any())).thenReturn(user);

        // When
        var response = userService.updateUser(userId,updateRequest);

        // Then
        Assertions.assertThat(response.getId()).isEqualTo("de2b8428-15fe-49cd-82c3");
        Assertions.assertThat(response.getUserName()).isEqualTo("john123");
        Assertions.assertThat(response.getFirstName()).isEqualTo("John");
        Assertions.assertThat(response.getDob()).isEqualTo("1990-01-01");
        Assertions.assertThat(response.getLastName()).isEqualTo("Doe");

        Assertions.assertThat(response.getRoles())
                .extracting("name", String.class)
                .contains("USER");

        Assertions.assertThat(response.getRoles())
                .extracting("description", String.class)
                .contains("User role");
    }

    @Test
    void undateUser_userNotFound_error() {
        // Give
        var userId = "de2b8428-15fe-49cd-82c3";
        Mockito.when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        // Then
        var exception = assertThrows(AppException.class,
                () -> userService.updateUser(userId,updateRequest));

        // Then
        Assertions.assertThat(exception.getErrorCode().getCode())
                .isEqualTo(1002);
        Assertions.assertThat(exception.getErrorCode().getMessage())
                .isEqualTo("User not found");

    }

    //delete User By Id
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteUser_validUser_success() {
        // Given
        String userId = "de2b8428-15fe-49cd-82c3";

        Mockito.when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        // When
        userService.deleteUser(userId);

        // Then
        Mockito.verify(userRepository, Mockito.times(1))
                .deleteById(userId);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteUser_userNotFound_error() {
        // Given
        String userId = "de2b8428-15fe-49cd-82c3";

        Mockito.when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        // Then
        var exception = assertThrows(AppException.class,
                () -> userService.deleteUser(userId));

        // Then
        Assertions.assertThat(exception.getErrorCode().getCode())
                .isEqualTo(1002);
        Assertions.assertThat(exception.getErrorCode().getMessage())
                .isEqualTo("User not found");
    }
}
