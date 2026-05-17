package com.community.health.controller;

import com.community.health.audit.AuditLog;
import com.community.health.common.ApiResponse;
import com.community.health.model.UserAccount;
import com.community.health.model.enums.Role;
import com.community.health.repository.ResidentProfileRepository;
import com.community.health.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserAccountRepository userAccountRepository;
    private final ResidentProfileRepository residentProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserAccountRepository userAccountRepository,
                          ResidentProfileRepository residentProfileRepository,
                          PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.residentProfileRepository = residentProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ApiResponse<Page<UserAccount>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (role != null && keyword != null && !keyword.trim().isEmpty()) {
            return ApiResponse.ok(userAccountRepository.findByUsernameContainingIgnoreCaseAndRole(keyword.trim(), role, pageable));
        } else if (role != null) {
            return ApiResponse.ok(userAccountRepository.findByRole(role, pageable));
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            return ApiResponse.ok(userAccountRepository.findByUsernameContainingIgnoreCase(keyword.trim(), pageable));
        }
        return ApiResponse.ok(userAccountRepository.findAll(pageable));
    }

    @PutMapping("/{id}/role")
    @AuditLog(module = "用户管理", action = "修改角色")
    public ApiResponse<String> updateRole(@PathVariable Long id, @RequestParam Role role) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        user.setRole(role);
        userAccountRepository.save(user);
        return ApiResponse.ok("角色修改成功", "ok");
    }

    @PutMapping("/{id}/status")
    @AuditLog(module = "用户管理", action = "修改状态")
    public ApiResponse<String> updateStatus(@PathVariable Long id, @RequestParam Boolean enabled) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        user.setEnabled(enabled);
        userAccountRepository.save(user);
        return ApiResponse.ok("状态修改成功", "ok");
    }

    @PutMapping("/{id}/password")
    @AuditLog(module = "用户管理", action = "重置密码")
    public ApiResponse<String> resetPassword(@PathVariable Long id, @RequestBody String newPassword) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userAccountRepository.save(user);
        return ApiResponse.ok("密码重置成功", "ok");
    }

    @DeleteMapping("/{id}")
    @AuditLog(module = "用户管理", action = "删除用户")
    public ApiResponse<String> delete(@PathVariable Long id) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        residentProfileRepository.findByUser(user).ifPresent(residentProfileRepository::delete);
        userAccountRepository.delete(user);
        return ApiResponse.ok("用户删除成功", "ok");
    }
}