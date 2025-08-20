package com.devtie.devteria.configuration;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.devtie.devteria.dto.response.ApiResponse;
import com.devtie.devteria.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JwtAuthencationEntryPoint implements AuthenticationEntryPoint {
    // Phuong thuc nay se duoc goi khi nguoi dung loi 401
    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;
        response.setStatus(errorCode.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE); // tra dinh dang json

        ApiResponse apiResponse = ApiResponse.builder() // tao dl tra ve ApiResponse
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        ObjectMapper objectMapper = new ObjectMapper(); // Jackson ObjectMapper to convert object to JSON
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse)); // chuyen doi apiResponse thanh chuoi
        // JSON
        response.flushBuffer();
    }
}
