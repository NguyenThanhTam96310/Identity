package com.devtie.devteria.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.time.LocalDate;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;

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

    @MockBean
    private UserRepository userRepository;

    private UserCreationRequest request;
    private UserResponse userResponse;
    private User user;
    private LocalDate dob;

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
    }

    @Test
    void createUser_validRequest_success() {
        // Given
        Mockito.when(userRepository.existsByUserName(anyString())).thenReturn(false);
        Mockito.when(userRepository.save(any())).thenReturn(user);

        // When
        var response = userService.createUser(request);

        // Then
        Assertions.assertThat(response.getId()).isEqualTo("de2b8428-15fe-49cd-82c3");
        Assertions.assertThat(response.getUserName()).isEqualTo("john123");
        Assertions.assertThat(response.getFirstName()).isEqualTo("John");
    }

    @Test
    void createUser_userExitsted_fail() {
        // Given
        Mockito.when(userRepository.existsByUserName(anyString())).thenReturn(true);

        // When
        var exception = assertThrows(AppException.class, () -> userService.createUser(request));
        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1001);
    }

    @Test
    @WithMockUser(username = "john123") // có thể hash role vào
    void getMyInfo_vailid_succuss() {
        // mock repository qua
        Mockito.when(userRepository.findByUserName(anyString())).thenReturn(Optional.of(user));

        // when
        var res = userService.getMyInfo();
        Assertions.assertThat(res.getUserName()).isEqualTo("john123");
        Assertions.assertThat(res.getId()).isEqualTo("de2b8428-15fe-49cd-82c3");
    }

    @Test
    @WithMockUser(username = "john123") // có thể hash role vào
    void getMyInfo_userNotFound_error() {
        // mock repository qua
        Mockito.when(userRepository.findByUserName(anyString())).thenReturn(Optional.ofNullable(null));

        // when
        var exception = assertThrows(AppException.class, () -> userService.getMyInfo());
        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1002);
    }
}
