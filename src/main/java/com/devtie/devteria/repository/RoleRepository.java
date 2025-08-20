package com.devtie.devteria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.devtie.devteria.entity.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {}
