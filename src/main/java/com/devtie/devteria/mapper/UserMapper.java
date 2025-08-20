package com.devtie.devteria.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.devtie.devteria.dto.request.UserCreationRequest;
import com.devtie.devteria.dto.request.UserUpdateRequest;
import com.devtie.devteria.dto.response.UserResponse;
import com.devtie.devteria.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserCreationRequest request);

    // @Mapping(source = "firstName", target = "lastName") // Khi get thif sẽ lấy
    // lastName từ firstName
    // @Mapping(target = "lastName", ignore = true) // khoong lấy lastName từ ra khi
    // đó lastName sẽ là null
    UserResponse toUserResponse(User user);

    @Mapping(target = "roles", ignore = true)
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}
