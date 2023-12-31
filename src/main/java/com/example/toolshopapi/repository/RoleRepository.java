package com.example.toolshopapi.repository;

import com.example.toolshopapi.model.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface RoleRepository extends JpaRepository<Role,Long> {
    Optional<Role> findAllByName(String name);

}
