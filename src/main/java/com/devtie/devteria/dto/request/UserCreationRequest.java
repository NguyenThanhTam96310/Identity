package com.devtie.devteria.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

import com.devtie.devteria.validator.DobConstraint;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {
    @Size(min = 3, message = "INVALID_REQUEST_USERNAME")
    String userName;

    @Size(min = 8, message = "INVALID_REQUEST_PASSWORD")
    String passWord;

    @Size(min = 2, message = "INVALID_REQUEST_FIRSTNAME")
    String firstName;

    @Size(min = 2, message = "INVALID_REQUEST_LASTNAME")
    String lastName;

    @DobConstraint(min = 16, message = "INVALID_DOB")
    LocalDate dob;
}
