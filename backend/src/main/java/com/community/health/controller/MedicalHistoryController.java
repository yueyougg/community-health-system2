package com.community.health.controller;

import com.community.health.audit.AuditLog;
import com.community.health.common.ApiResponse;
import com.community.health.dto.MedicalHistoryDetailRequest;
import com.community.health.dto.MedicalHistoryDetailResponse;
import com.community.health.model.MedicalHistory;
import com.community.health.model.MedicalHistoryAllergyRecord;
import com.community.health.model.MedicalHistoryDisease;
import com.community.health.model.MedicalHistoryGeneticRecord;
import com.community.health.model.ResidentProfile;
import com.community.health.model.UserAccount;
import com.community.health.repository.MedicalHistoryAllergyRecordRepository;
import com.community.health.repository.MedicalHistoryDiseaseRepository;
import com.community.health.repository.MedicalHistoryGeneticRecordRepository;
import com.community.health.repository.MedicalHistoryRepository;
import com.community.health.repository.ResidentProfileRepository;
import com.community.health.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/medical-histories")
public class MedicalHistoryController {

    private final MedicalHistoryRepository medicalHistoryRepository;
    private final MedicalHistoryDiseaseRepository medicalHistoryDiseaseRepository;
    private final MedicalHistoryGeneticRecordRepository medicalHistoryGeneticRecordRepository;
    private final MedicalHistoryAllergyRecordRepository medicalHistoryAllergyRecordRepository;
    private final ResidentProfileRepository residentProfileRepository;
    private final UserAccountRepository userAccountRepository;

    public MedicalHistoryController(MedicalHistoryRepository medicalHistoryRepository,
                                    MedicalHistoryDiseaseRepository medicalHistoryDiseaseRepository,
                                    MedicalHistoryGeneticRecordRepository medicalHistoryGeneticRecordRepository,
                                    MedicalHistoryAllergyRecordRepository medicalHistoryAllergyRecordRepository,
                                    ResidentProfileRepository residentProfileRepository,
                                    UserAccountRepository userAccountRepository) {
        this.medicalHistoryRepository = medicalHistoryRepository;
        this.medicalHistoryDiseaseRepository = medicalHistoryDiseaseRepository;
        this.medicalHistoryGeneticRecordRepository = medicalHistoryGeneticRecordRepository;
        this.medicalHistoryAllergyRecordRepository = medicalHistoryAllergyRecordRepository;
        this.residentProfileRepository = residentProfileRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping
    public ApiResponse<List<MedicalHistoryDetailResponse>> list(@RequestParam(required = false) Long residentId) {
        if (residentId == null) {
            List<MedicalHistoryDetailResponse> all = medicalHistoryRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
                    .stream()
                    .map(this::toDetailResponse)
                    .toList();
            return ApiResponse.ok(all);
        }
        List<MedicalHistoryDetailResponse> list = medicalHistoryRepository.findFirstByResidentIdOrderByIdDesc(residentId)
                .map(this::toDetailResponse)
                .map(List::of)
                .orElse(List.of());
        return ApiResponse.ok(list);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('RESIDENT')")
    public ApiResponse<List<MedicalHistoryDetailResponse>> myMedicalHistory() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccount user = userAccountRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        ResidentProfile profile = residentProfileRepository.findByUser(user)
                .orElseThrow(() -> new EntityNotFoundException("未找到您的健康档案"));
        
        return ApiResponse.ok(medicalHistoryRepository.findFirstByResidentIdOrderByIdDesc(profile.getId())
                .map(this::toDetailResponse)
                .map(List::of)
                .orElse(List.of()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','RESIDENT')")
    @AuditLog(module = "健康信息", action = "创建病史")
    public ApiResponse<MedicalHistoryDetailResponse> create(@RequestBody MedicalHistoryDetailRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isResident = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_RESIDENT"));
        String sourceType;
        Long residentId = request.getResidentId();
        if (isResident) {
            UserAccount user = userAccountRepository.findByUsername(auth.getName()).orElseThrow();
            ResidentProfile profile = residentProfileRepository.findByUser(user).orElseThrow();
            residentId = profile.getId();
            sourceType = "RESIDENT_SELF";
        } else {
            sourceType = "DOCTOR_ENTRY";
        }
        if (residentId == null) {
            throw new IllegalArgumentException("residentId不能为空");
        }

        MedicalHistory db = medicalHistoryRepository.findFirstByResidentIdOrderByIdDesc(residentId)
                .orElseGet(MedicalHistory::new);
        db.setResidentId(residentId);
        db.setSmokingHabit(request.getSmokingHabit());
        db.setDrinkingHabit(request.getDrinkingHabit());
        db.setDietHabit(request.getDietHabit());
        db.setExerciseHabit(request.getExerciseHabit());
        db.setSourceType(sourceType);
        MedicalHistory saved = medicalHistoryRepository.save(db);
        replaceChildren(saved.getId(), request);
        return ApiResponse.ok("保存成功", toDetailResponse(saved));
    }

    @PutMapping("/me/{id}")
    @PreAuthorize("hasRole('RESIDENT')")
    @AuditLog(module = "健康信息", action = "居民更新病史")
    @Transactional
    public ApiResponse<MedicalHistoryDetailResponse> updateMyHistory(@PathVariable Long id, @RequestBody MedicalHistoryDetailRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccount user = userAccountRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        ResidentProfile profile = residentProfileRepository.findByUser(user)
                .orElseThrow(() -> new EntityNotFoundException("未找到您的健康档案"));

        MedicalHistory db = medicalHistoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("健康信息不存在"));
        if (!profile.getId().equals(db.getResidentId())) {
            throw new IllegalArgumentException("无权修改该健康史记录");
        }
        db.setSmokingHabit(request.getSmokingHabit());
        db.setDrinkingHabit(request.getDrinkingHabit());
        db.setDietHabit(request.getDietHabit());
        db.setExerciseHabit(request.getExerciseHabit());
        db.setSourceType("RESIDENT_SELF");
        MedicalHistory saved = medicalHistoryRepository.save(db);
        replaceChildren(saved.getId(), request);
        return ApiResponse.ok("更新成功", toDetailResponse(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @AuditLog(module = "健康信息", action = "更新病史")
    @Transactional
    public ApiResponse<MedicalHistoryDetailResponse> update(@PathVariable Long id, @RequestBody MedicalHistoryDetailRequest request) {
        MedicalHistory db = medicalHistoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("健康信息不存在"));
        db.setResidentId(request.getResidentId() == null ? db.getResidentId() : request.getResidentId());
        db.setSmokingHabit(request.getSmokingHabit());
        db.setDrinkingHabit(request.getDrinkingHabit());
        db.setDietHabit(request.getDietHabit());
        db.setExerciseHabit(request.getExerciseHabit());
        db.setSourceType(request.getSourceType() == null ? "DOCTOR_ENTRY" : request.getSourceType());
        MedicalHistory saved = medicalHistoryRepository.save(db);
        replaceChildren(saved.getId(), request);
        return ApiResponse.ok("更新成功", toDetailResponse(saved));
    }

    @Transactional
    public void replaceChildren(Long medicalHistoryId, MedicalHistoryDetailRequest request) {
        medicalHistoryDiseaseRepository.deleteByMedicalHistoryId(medicalHistoryId);
        medicalHistoryGeneticRecordRepository.deleteByMedicalHistoryId(medicalHistoryId);
        medicalHistoryAllergyRecordRepository.deleteByMedicalHistoryId(medicalHistoryId);

        if (request.getDiseases() != null) {
            request.getDiseases().stream()
                    .filter(i -> i != null && i.getDiseaseName() != null && !i.getDiseaseName().isBlank())
                    .forEach(i -> {
                        MedicalHistoryDisease d = new MedicalHistoryDisease();
                        d.setMedicalHistoryId(medicalHistoryId);
                        d.setDiseaseName(i.getDiseaseName().trim());
                        d.setTreatmentStatus(i.getTreatmentStatus());
                        d.setCheckedAt(i.getCheckedAt());
                        medicalHistoryDiseaseRepository.save(d);
                    });
        }
        if (request.getGeneticHistories() != null) {
            request.getGeneticHistories().stream()
                    .filter(i -> i != null
                            && i.getDiseaseName() != null && !i.getDiseaseName().isBlank()
                            && i.getRelationToResident() != null && !i.getRelationToResident().isBlank())
                    .forEach(i -> {
                        MedicalHistoryGeneticRecord g = new MedicalHistoryGeneticRecord();
                        g.setMedicalHistoryId(medicalHistoryId);
                        g.setDiseaseName(i.getDiseaseName().trim());
                        g.setRelationToResident(i.getRelationToResident().trim());
                        medicalHistoryGeneticRecordRepository.save(g);
                    });
        }
        if (request.getAllergies() != null) {
            request.getAllergies().stream()
                    .filter(i -> i != null && i.getAllergen() != null && !i.getAllergen().isBlank())
                    .forEach(i -> {
                        MedicalHistoryAllergyRecord a = new MedicalHistoryAllergyRecord();
                        a.setMedicalHistoryId(medicalHistoryId);
                        a.setAllergen(i.getAllergen().trim());
                        a.setAllergicReaction(i.getAllergicReaction());
                        medicalHistoryAllergyRecordRepository.save(a);
                    });
        }
    }

    private MedicalHistoryDetailResponse toDetailResponse(MedicalHistory mh) {
        MedicalHistoryDetailResponse resp = new MedicalHistoryDetailResponse();
        resp.setId(mh.getId());
        resp.setResidentId(mh.getResidentId());
        resp.setSmokingHabit(mh.getSmokingHabit());
        resp.setDrinkingHabit(mh.getDrinkingHabit());
        resp.setDietHabit(mh.getDietHabit());
        resp.setExerciseHabit(mh.getExerciseHabit());
        resp.setSourceType(mh.getSourceType());
        resp.setCreatedAt(mh.getCreatedAt());
        resp.setUpdatedAt(mh.getUpdatedAt());

        resp.setDiseases(medicalHistoryDiseaseRepository.findByMedicalHistoryIdOrderByIdAsc(mh.getId()).stream().map(i -> {
            MedicalHistoryDetailResponse.DiseaseItem d = new MedicalHistoryDetailResponse.DiseaseItem();
            d.setId(i.getId());
            d.setDiseaseName(i.getDiseaseName());
            d.setTreatmentStatus(i.getTreatmentStatus());
            d.setCheckedAt(i.getCheckedAt());
            return d;
        }).toList());
        resp.setGeneticHistories(medicalHistoryGeneticRecordRepository.findByMedicalHistoryIdOrderByIdAsc(mh.getId()).stream().map(i -> {
            MedicalHistoryDetailResponse.GeneticItem g = new MedicalHistoryDetailResponse.GeneticItem();
            g.setId(i.getId());
            g.setDiseaseName(i.getDiseaseName());
            g.setRelationToResident(i.getRelationToResident());
            return g;
        }).toList());
        resp.setAllergies(medicalHistoryAllergyRecordRepository.findByMedicalHistoryIdOrderByIdAsc(mh.getId()).stream().map(i -> {
            MedicalHistoryDetailResponse.AllergyItem a = new MedicalHistoryDetailResponse.AllergyItem();
            a.setId(i.getId());
            a.setAllergen(i.getAllergen());
            a.setAllergicReaction(i.getAllergicReaction());
            return a;
        }).toList());
        return resp;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @AuditLog(module = "健康信息", action = "删除病史")
    public ApiResponse<String> delete(@PathVariable Long id) {
        medicalHistoryRepository.deleteById(id);
        return ApiResponse.ok("删除成功", "ok");
    }
}