package com.community.health.controller;

import com.community.health.common.ApiResponse;
import com.community.health.dto.AuthResponse;
import com.community.health.dto.LoginRequest;
import com.community.health.dto.RegisterRequest;
import com.community.health.model.LoginRecord;
import com.community.health.model.UserAccount;
import com.community.health.model.enums.Role;
import com.community.health.repository.LoginRecordRepository;
import com.community.health.repository.UserAccountRepository;
import com.community.health.security.JwtTokenProvider;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserAccountRepository userAccountRepository;
    private final LoginRecordRepository loginRecordRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(UserAccountRepository userAccountRepository,
                          LoginRecordRepository loginRecordRepository,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider) {
        this.userAccountRepository = userAccountRepository;
        this.loginRecordRepository = loginRecordRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public ApiResponse<String> register(@Valid @RequestBody RegisterRequest request) {
        if (userAccountRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }

        Role role = request.getRole() != null ? request.getRole() : Role.RESIDENT;

        // 校验邀请码
        if (role != Role.RESIDENT) {
            validateInvitationCode(role.name(), request.getInvitationCode());
        }

        UserAccount account = new UserAccount();
        account.setUsername(request.getUsername());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setRole(role);
        account.setEnabled(true);
        userAccountRepository.save(account);

        return ApiResponse.ok("注册成功", "ok");
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        // 先根据用户名查角色，因为登录也需要邀请码
        UserAccount user = userAccountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("用户不存在，请检查用户名是否正确"));

        // 检查用户是否被禁用
        if (!user.getEnabled()) {
            throw new IllegalArgumentException("用户已被禁用，请联系管理员");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("RESIDENT");

        // 如果请求中包含了角色，进行校验
        if (request.getRole() != null && !request.getRole().isEmpty()) {
            if (!role.equals(request.getRole())) {
                 throw new IllegalArgumentException("角色不匹配，请选择正确的角色登录");
            }
        }

        // 保存登录记录
        LoginRecord loginRecord = new LoginRecord();
        loginRecord.setUser(user);
        loginRecord.setLoginRole(role);
        loginRecord.setIpAddress(httpServletRequest.getRemoteAddr());
        loginRecordRepository.save(loginRecord);

        String token = jwtTokenProvider.generateToken(authentication);
        return ApiResponse.ok(new AuthResponse(token, request.getUsername(), role));
    }

    private void validateInvitationCode(String role, String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("该角色登录/注册需要邀请码");
        }

        Map<String, String> codes = Map.of(
            "ADMIN", "ADMIN888",
            "DOCTOR", "DOC666",
            "PUBLIC_HEALTH_MANAGER", "PHM777"
        );

        String expected = codes.get(role);
        if (expected != null && !expected.equals(code)) {
            throw new IllegalArgumentException("邀请码不正确");
        }
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("未登录");
        }
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("RESIDENT");
        return ApiResponse.ok(Map.of("username", authentication.getName(), "role", role));
    }
}