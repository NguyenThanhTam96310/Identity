package com.devtie.devteria.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.devtie.devteria.dto.request.RoleRequest;
import com.devtie.devteria.dto.response.RoleResponse;
import com.devtie.devteria.entity.Role;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "permissions", ignore = true) // Mình không ánh xạ permissions từ RoleRequest sang Role vì nó là
    // Set<String> và không phải là một entity
    Role toRole(RoleRequest request);

    RoleResponse toRoleResponse(Role role);
}
