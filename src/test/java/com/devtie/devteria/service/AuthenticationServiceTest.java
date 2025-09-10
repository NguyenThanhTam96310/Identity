package com.devtie.devteria.service;

import com.devtie.devteria.dto.request.*;
import com.devtie.devteria.dto.response.*;
import com.devtie.devteria.entity.InvalidatedToken;
import com.devtie.devteria.entity.Role;
import com.devtie.devteria.entity.User;
import com.devtie.devteria.exception.AppException;
import com.devtie.devteria.exception.ErrorCode;
import com.devtie.devteria.repository.InvalidatedTokenRepository;
import com.devtie.devteria.repository.RoleRepository;
import com.devtie.devteria.repository.UserRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.text.ParseException;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource("/test.properties")
public class AuthenticationServiceTest {
    @Autowired
    private UserService userService;

    @MockitoBean
    private InvalidatedTokenRepository invalidatedTokenRepository;

    @Autowired
    private AuthencationService authenticationService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private PasswordEncoder encoder;

    private User user;
    private AuthenticationRequest authenticationRequest;
    private AuthenticationResponse authenticationResponse;
    private JWTClaimsSet  claimsSet;
    private LogoutRequest logoutRequest;
    private IntrospectRequest introspectRequest;
    private IntrospectResponse introspectResponse;
    private RefreshRequest refreshRequest;

    @BeforeEach
        // chạy phương thức này đầu tiên trc khi chạy test kh
    void initData() {
        PasswordEncoder realEncoder = new BCryptPasswordEncoder(10);
        user = User.builder()
                .id("de2b8428-15fe-49cd-82c3")
                .userName("name")
                .passWord(realEncoder.encode("12345678"))
                .roles(Set.of(Role.builder().name("USER").build()))
                .build();
        authenticationRequest = AuthenticationRequest.builder()
                .userName("name")
                .passWord("12345678")
                .build();
        authenticationResponse = AuthenticationResponse.builder()
                .authenticated(true)
                .token("eyJhbGciOiJIUzUxMiJ9")
                .build();
        introspectRequest = IntrospectRequest.builder()
                .token(authenticationResponse.getToken())
                .build();
        introspectResponse = IntrospectResponse.builder()
                .valid(true)
                .build();
        logoutRequest = LogoutRequest.builder()
                .token("aaa.bbb.ccc")
                .build();
        claimsSet = new JWTClaimsSet.Builder()
                .jwtID("jitExpired")
                .expirationTime(new Date(System.currentTimeMillis() + 3600_000)) // hết hạn
                .build();
        refreshRequest = RefreshRequest.builder()
                .token("aaa.bbb.ccc")
                .build();
    }
    //authenticate
    @Test
    void authenticate_validRequest_success() {
        // Given
        Mockito.when(userRepository.findByUserName("name"))
                .thenReturn(Optional.of(user));
        // password
        Mockito.when(encoder.matches(authenticationRequest.getPassWord(), user.getPassWord()))
                .thenReturn(true);
        // When
        var response = authenticationService.authenticate(authenticationRequest);

        // Then
        Assertions.assertThat(response.isAuthenticated()).isTrue();
        Assertions.assertThat(response.getToken()).isNotBlank();
    }
    @Test
    void authenticate_userNotFound_error() {
        // Given
        Mockito.when(userRepository.findByUserName(anyString())).thenReturn(Optional.empty());

        // When
        var exception = assertThrows(AppException.class, () -> authenticationService.authenticate(authenticationRequest));

        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1002);
        Assertions.assertThat(exception.getErrorCode().getMessage()).isEqualTo("User not found");
    }
    @Test
    void authenticate_usernameOrPassWord_error() {
        // Given
        authenticationRequest.setPassWord("Invalid");
        Mockito.when(userRepository.findByUserName("name"))
                .thenReturn(Optional.of(user));
        Mockito.when(encoder.matches(authenticationRequest.getPassWord(), user.getPassWord()))
                .thenReturn(true);

        // When
        var exception = assertThrows(AppException.class, () -> authenticationService.authenticate(authenticationRequest));

        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1006);
        Assertions.assertThat(exception.getErrorCode().getMessage()).isEqualTo("Username or password does not match");
    }

    //LOGOUT
    @Test
    void logout_validRequest_success() throws ParseException, JOSEException {
        // Given
        SignedJWT signedJWT = Mockito.mock(SignedJWT.class);
        Mockito.when(signedJWT.getJWTClaimsSet()).thenReturn(claimsSet);

        // Spy service để mock verityToken
        AuthencationService spyService = Mockito.spy(authenticationService);
        Mockito.doReturn(signedJWT).when(spyService).verityToken("aaa.bbb.ccc", true);

        // Mock hành vi của invalidatedTokenRepository để không ném lỗi
        Mockito.when(invalidatedTokenRepository.existsById("jitExpired")).thenReturn(false);
        // When
        spyService.logout(logoutRequest);

        // Then: verify repository.save được gọi với InvalidatedToken có jit và expiry đúng
        ArgumentCaptor<InvalidatedToken> captor = ArgumentCaptor.forClass(InvalidatedToken.class);
        Mockito.verify(invalidatedTokenRepository, Mockito.times(1)).save(captor.capture());

        InvalidatedToken savedToken = captor.getValue();
        Assertions.assertThat(savedToken.getId()).isEqualTo("jitExpired");
        Assertions.assertThat(savedToken.getExpiryTime()).isCloseTo(new Date(System.currentTimeMillis() + 3600_000),1000);
    }
    @Test
    void logout_validRequest_error() throws ParseException, JOSEException {
        // Given
        AuthencationService spyService = Mockito.spy(authenticationService);

        // Giả lập verityToken ném AppException (token hết hạn)
        Mockito.doThrow(new AppException(ErrorCode.TOKEN_ALREADY_EXPIRED))
                .when(spyService).verityToken(anyString(), Mockito.eq(true));

        // When
        var exception = assertThrows(AppException.class, () -> spyService.logout(logoutRequest));

        // Then
        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1009);
        Assertions.assertThat(exception.getErrorCode().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(exception.getErrorCode().getMessage()).isEqualTo("Token already expired");

        // verify repository không bị gọi
        Mockito.verify(invalidatedTokenRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void refreshToken_validRequest_success() throws Exception {
        // Given
        SignedJWT signedJWT = Mockito.mock(SignedJWT.class);
        Mockito.when(signedJWT.getJWTClaimsSet()).thenReturn(claimsSet);

        AuthencationService spyService = Mockito.spy(authenticationService);
        Mockito.doReturn(signedJWT).when(spyService).verityToken("aaa.bbb.ccc", true);

        // Giả lập claims
        JWTClaimsSet mockClaims = new JWTClaimsSet.Builder()
                .jwtID("jit123")
                .expirationTime(new Date(System.currentTimeMillis() + 3600_000))
                .subject("name")
                .build();
        Mockito.when(signedJWT.getJWTClaimsSet()).thenReturn(mockClaims);

        // Giả lập user tồn tại
        Mockito.when(userRepository.findByUserName("name")).thenReturn(Optional.of(user));

        // Giả lập generateToken trả về token mới
        Mockito.doReturn("new-token-xyz").when(spyService).generateToken(user);

        // When
        AuthenticationResponse response = spyService.refreshToken(refreshRequest);

        // Then
        Assertions.assertThat(response.isAuthenticated()).isTrue();
        Assertions.assertThat(response.getToken()).isEqualTo("new-token-xyz");

        // verify repo save token cũ
        ArgumentCaptor<InvalidatedToken> captor = ArgumentCaptor.forClass(InvalidatedToken.class);
        Mockito.verify(invalidatedTokenRepository, Mockito.times(1)).save(captor.capture());
        Assertions.assertThat(captor.getValue().getId()).isEqualTo("jit123");
    }
    @Test
    void refreshToken_userNotFound_error() throws Exception {
        // Given
        SignedJWT signedJWT = Mockito.mock(SignedJWT.class);

        JWTClaimsSet mockClaims = new JWTClaimsSet.Builder()
                .jwtID("jit123")
                .expirationTime(new Date(System.currentTimeMillis() + 3600_000))
                .subject("ghostUser")
                .build();
        Mockito.when(signedJWT.getJWTClaimsSet()).thenReturn(mockClaims);

        AuthencationService spyService = Mockito.spy(authenticationService);
        Mockito.doReturn(signedJWT).when(spyService).verityToken("aaa.bbb.ccc", true);

        // User không tồn tại
        Mockito.when(userRepository.findByUserName("ghostUser")).thenReturn(Optional.empty());
        // When + Then
        AppException exception = assertThrows(AppException.class, () -> spyService.refreshToken(refreshRequest));

        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1007);
        Assertions.assertThat(exception.getErrorCode().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        Assertions.assertThat(exception.getErrorCode().getMessage()).isEqualTo("Unauthenticated");
        Mockito.verify(invalidatedTokenRepository, Mockito.times(1)).save(Mockito.any());
    }

}
