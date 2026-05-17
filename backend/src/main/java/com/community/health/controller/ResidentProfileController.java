package com.community.health.controller;

import com.community.health.audit.AuditLog;
import com.community.health.common.ApiResponse;
import com.community.health.dto.ResidentProfileCreateRequest;
import com.community.health.model.MedicalHistory;
import com.community.health.model.ResidentProfile;
import com.community.health.model.UserAccount;
import com.community.health.model.enums.Role;
import com.community.health.repository.MedicalHistoryRepository;
import com.community.health.repository.ResidentProfileRepository;
import com.community.health.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@RestController
@RequestMapping("/api/residents")
public class ResidentProfileController {


    private final ResidentProfileRepository residentProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final MedicalHistoryRepository medicalHistoryRepository;
    private final Random random = new Random();

    public ResidentProfileController(ResidentProfileRepository residentProfileRepository,
                                     UserAccountRepository userAccountRepository,
                                     MedicalHistoryRepository medicalHistoryRepository) {
        this.residentProfileRepository = residentProfileRepository;
        this.userAccountRepository = userAccountRepository;
        this.medicalHistoryRepository = medicalHistoryRepository;
    }

    private String generateArchiveNo() {
        String prefix = "CH-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String archiveNo;
        do {
            archiveNo = prefix + "-" + String.format("%04d", random.nextInt(10000));
        } while (residentProfileRepository.findByArchiveNo(archiveNo).isPresent());
        return archiveNo;
    }

    @GetMapping
    public ApiResponse<List<ResidentProfile>> list(@RequestParam(required = false) String keyword) {
        // 流程：未输入检索词时返回全部，输入后按姓名/档案编号模糊匹配
        if (keyword == null || keyword.trim().isEmpty()) {
            return ApiResponse.ok(residentProfileRepository.findAll(Sort.by(Sort.Direction.DESC, "id")));
        }
        return ApiResponse.ok(residentProfileRepository.searchByKeyword(keyword.trim()));
    }

    @GetMapping("/available-users")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ApiResponse<List<Map<String, Object>>> availableUsers() {
        List<Map<String, Object>> users = userAccountRepository.findUnboundEnabledUsersByRole(Role.RESIDENT).stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "username", u.getUsername()
                ))
                .toList();
        return ApiResponse.ok(users);
    }

    @GetMapping("/{id}")
    public ApiResponse<ResidentProfile> detail(@PathVariable Long id) {
        return ApiResponse.ok(residentProfileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("居民档案不存在")));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('RESIDENT')")
    public ApiResponse<ResidentProfile> myProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccount user = userAccountRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        ResidentProfile profile = residentProfileRepository.findByUser(user)
                .orElseThrow(() -> new EntityNotFoundException("未找到您的健康档案，请联系社区医生建立档案"));
        return ApiResponse.ok(profile);
    }

    @PostMapping("/me")
    @PreAuthorize("hasRole('RESIDENT')")
    @AuditLog(module = "居民档案", action = "自助创建")
    public ApiResponse<ResidentProfile> createMyProfile(@Valid @RequestBody ResidentProfileCreateRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccount user = userAccountRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));

        // 1. 检查当前用户是否已经关联了档案
        if (residentProfileRepository.findByUser(user).isPresent()) {
            throw new IllegalArgumentException("您的档案已存在，请勿重复创建");
        }

        ResidentProfile profile = new ResidentProfile();
        profile.setUser(user);
        profile.setArchiveNo(generateArchiveNo());

        String name = request.getName();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("姓名不能为空");
        }
        profile.setName(name.trim());
        profile.setGender(request.getGender());
        profile.setBirthDate(request.getBirthDate());
        if (request.getIdCard() != null && !request.getIdCard().isBlank()) {
            String idCard = request.getIdCard().trim();
            residentProfileRepository.findByIdCard(idCard).ifPresent(existed -> {
                throw new IllegalArgumentException("该身份证号对应的档案已存在");
            });
            profile.setIdCard(idCard);
        }
        profile.setPhone(request.getPhone());
        profile.setAddress(request.getAddress());
        profile.setOccupation(request.getOccupation());
        profile.setArchiveDate(request.getArchiveDate() != null ? request.getArchiveDate() : LocalDate.now());
        profile.setArchiveOrg(request.getArchiveOrg());

        ResidentProfile savedProfile = residentProfileRepository.save(profile);
        // 自动创建空的健康史记录
        MedicalHistory medicalHistory = new MedicalHistory();
        medicalHistory.setResidentId(savedProfile.getId());
        medicalHistory.setSourceType("SYSTEM_AUTO");
        medicalHistoryRepository.save(medicalHistory);
        return ApiResponse.ok("档案创建成功", savedProfile);
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('RESIDENT')")
    @AuditLog(module = "居民档案", action = "自助更新")
    public ApiResponse<ResidentProfile> updateMyProfile(@Valid @RequestBody ResidentProfileCreateRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccount user = userAccountRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));

        ResidentProfile profile = residentProfileRepository.findByUser(user)
                .orElseThrow(() -> new EntityNotFoundException("未找到您的健康档案，请先建立档案"));

        if (request.getName() != null && !request.getName().isBlank()) {
            profile.setName(request.getName().trim());
        }

        if (request.getIdCard() != null) {
            String nextIdCard = request.getIdCard().trim();
            if (nextIdCard.isBlank()) {
                // 清空身份证号
                profile.setIdCard(null);
            } else if (!nextIdCard.equals(profile.getIdCard())) {
                // 更新为新的身份证号
                residentProfileRepository.findByIdCard(nextIdCard).ifPresent(existed -> {
                    if (!existed.getId().equals(profile.getId())) {
                        throw new IllegalArgumentException("该身份证号对应的档案已存在");
                    }
                });
                profile.setIdCard(nextIdCard);
            }
        }
        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone().trim());
        }
        
        profile.setGender(request.getGender());
        profile.setBirthDate(request.getBirthDate());
        profile.setAddress(request.getAddress());
        profile.setOccupation(request.getOccupation());
        if (request.getArchiveDate() != null) {
            profile.setArchiveDate(request.getArchiveDate());
        }
        if (request.getArchiveOrg() != null) {
            profile.setArchiveOrg(request.getArchiveOrg());
        }

        return ApiResponse.ok("档案更新成功", residentProfileRepository.save(profile));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @AuditLog(module = "居民档案", action = "创建")
    public ApiResponse<ResidentProfile> create(@RequestBody ResidentProfileCreateRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("新增居民档案必须选择系统居民用户");
        }
        UserAccount user = userAccountRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("关联的用户不存在"));
        if (user.getRole() != Role.RESIDENT) {
            throw new IllegalArgumentException("仅可选择居民角色用户建档");
        }
        if (residentProfileRepository.findByUser(user).isPresent()) {
            throw new IllegalArgumentException("该用户已有关联的健康档案");
        }

        ResidentProfile residentProfile = new ResidentProfile();
        residentProfile.setUser(user);
        residentProfile.setName(request.getName());
        residentProfile.setGender(request.getGender());
        residentProfile.setBirthDate(request.getBirthDate());
        residentProfile.setIdCard(request.getIdCard());
        residentProfile.setPhone(request.getPhone());
        residentProfile.setAddress(request.getAddress());
        residentProfile.setOccupation(request.getOccupation());
        residentProfile.setArchiveDate(request.getArchiveDate());
        residentProfile.setArchiveOrg(request.getArchiveOrg());

        // 1. 检查身份证号是否已存在
        if (residentProfile.getIdCard() != null && !residentProfile.getIdCard().isBlank()) {
            if (residentProfileRepository.findByIdCard(residentProfile.getIdCard()).isPresent()) {
                throw new IllegalArgumentException("该身份证号对应的档案已存在");
            }
        }
        if (residentProfile.getName() == null || residentProfile.getName().isBlank()) {
            throw new IllegalArgumentException("姓名不能为空");
        }

        if (residentProfile.getArchiveNo() == null || residentProfile.getArchiveNo().isBlank()) {
            residentProfile.setArchiveNo(generateArchiveNo());
        }
        if (residentProfile.getArchiveDate() == null) {
            residentProfile.setArchiveDate(LocalDate.now());
        }
        ResidentProfile saved = residentProfileRepository.save(residentProfile);
        // 自动创建空的健康史记录
        MedicalHistory medicalHistory = new MedicalHistory();
        medicalHistory.setResidentId(saved.getId());
        medicalHistory.setSourceType("SYSTEM_AUTO");
        medicalHistoryRepository.save(medicalHistory);
        log.info("Saved resident profile: {} with ID: {}", saved.getName(), saved.getId());
        return ApiResponse.ok("创建成功", saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @AuditLog(module = "居民档案", action = "更新")
    public ApiResponse<ResidentProfile> update(@PathVariable Long id, @RequestBody ResidentProfile request) {
        ResidentProfile db = residentProfileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("居民档案不存在"));
        // 档案编号一旦生成不可修改
        // db.setArchiveNo(request.getArchiveNo());
        db.setName(request.getName());
        db.setGender(request.getGender());
        db.setBirthDate(request.getBirthDate());
        db.setIdCard(request.getIdCard());
        db.setPhone(request.getPhone());
        db.setAddress(request.getAddress());
        db.setOccupation(request.getOccupation());
        db.setArchiveDate(request.getArchiveDate());
        db.setArchiveOrg(request.getArchiveOrg());
        return ApiResponse.ok("更新成功", residentProfileRepository.save(db));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @AuditLog(module = "居民档案", action = "删除")
    public ApiResponse<String> delete(@PathVariable Long id) {
        residentProfileRepository.deleteById(id);
        return ApiResponse.ok("删除成功", "ok");
    }
}