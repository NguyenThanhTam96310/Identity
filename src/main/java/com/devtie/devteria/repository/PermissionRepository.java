package com.devtie.devteria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.devtie.devteria.entity.Permission;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {}
