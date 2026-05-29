package com.yuwen.magiccube.repository;

import com.yuwen.magiccube.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    // 按用户名查询用户
    Optional<User> findByUsername(String username);
    // 按班级查询所有学生
    List<User> findByClassId(String classId);
    // 按角色查询所有用户
    List<User> findByRole(String role);
}