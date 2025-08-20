package com.devtie.devteria.mapper;

import org.mapstruct.Mapper;

import com.devtie.devteria.dto.request.PermissionRequest;
import com.devtie.devteria.dto.response.PermissionResponse;
import com.devtie.devteria.entity.Permission;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionRequest request);

    PermissionResponse toPermissionResponse(Permission permission);
}
